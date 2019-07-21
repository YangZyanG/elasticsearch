一、关于elasticsearch写入数据的流程
大概的过程如下：
新增数据操作 -> 内存缓冲区buffer -> segment -> refresh -> 系统缓存
elasticsearch底层基于lucene实现，其中es索引中的每个分片都会有一个lucene索引，相当于一个lucene索引在es中成为了一个shard，一个es索引就是分片的集合。
写入数据的时候，数据会先写入buffer内存缓冲区，然后每隔一定时间(默认1s)refresh一次，就将buffer中的数据以新的segment存入到系统缓存中。
一旦segment写入系统缓存，就立即打开该segment供搜索使用，最后清空当前buffer区，等待接受新的数据。

es中每个shard每秒都会自动refresh一次，所以es是近实时的，数据插入到可供搜索的时间间隔默认是1s。
我们可以设置refresh时间间隔，例如：
PUT /ik_test/_settings
{
  "refresh_interval":"20s"
}
这样设置之后，新增、更新或删除的数据会每隔20s才会更新到磁盘。当业务逻辑对数据的实时性要求不高时，我们可以适当增大refresh的时间间隔，提高效率。
当你的生产环境中新建一个较大的索引时，要添加较多的数据，我们可以先关闭自动刷新功能，等数据导入完成后，再改回来。
PUT /ik_test/_settings
{
  "refresh_interval":"-1"
}

总结一下：
refresh的作用就是将内存缓冲区buffer中的数据一次性写入到一个新的segment中，但此时没有调用fsync，即数据还没有写到磁盘上而是在内存中，所以内存中的数据可能丢失。
但此时这个segment已经可以被用来搜索了。

二、关于数据持久化到磁盘的过程
当索引数据写入缓冲区buffer的同时，也写入到translog日志文件中了。
每隔一定时间(默认30分钟)，或者当translog文件达到一定大小时(默认512M)，发生flush操作，将translog文件中所有的segment通过fsync强制刷到磁盘上。
将此次写入磁盘的所有segment记录到提交点(commit point)，最后删除当前translog文件，创建新的translog文件接收新的请求。

translog储存了上一次flush到当前时间的所有数据变更。
当es发生故障或者服务器宕机，重启es之后，es将根据磁盘中的commit point去加载已经写入磁盘的segment到内存，并重新执行translog文件中所有的操作，从而保证数据的一致性。
为了保证数据不丢失，就要保证translog的安全，所以es每隔5s(默认)就会将translog中的数据通过fsync强制刷新到磁盘上。
es对整个translog的操作与redis的持久化有点类似，aof每秒append操作到append.aof，RBD每15分钟全量备份一次。
es提高数据安全的同时，降低了一点性能。
es2.0之后，每次写请求(index、update、delete、bulk等)完成时，都会触发translog的fsync操作，然后才会返回200状态的响应。
频繁地执行fsync操作必然会影响性能，如果允许小部分数据数据的丢失，可设置异步刷新translog并增大translog刷新时间的间隔：
PUT /ik_test/_settings
{
  "index.translog.durability":"async",
  "index.translog.sync_interval":"20s"
}

三、关于segment文件的归并
由上述近实时性搜索的描述, 可知es默认每秒都会产生一个新的segment文件, 而每次搜索时都要遍历所有的segment, 这非常影响搜索性能。
为解决这一问题，es会对这些零散的segment进行merge(归并)操作，尽量让索引中只保有少量的、体积较大的segment文件。
归并流程：
选择一些有相似大小的segment，merge成一个大的segment。
将新的segment刷新到磁盘上。
写一个新的commit point，包括了新的segment，并删除旧的segment。
打开新的segment，完成搜索请求的转移。
删除旧的小segment。

segment的归并是一个非常消耗系统CPU和磁盘IO资源的任务，所以es对归并线程提供了限速机制，确保这个任务不会过分影响到其他任务。
默认是20mb，这对写入量较大、磁盘转速较高的服务器来说明显过低，我们这里改成100mb：
PUT _cluster/settings
{
    "persistent" : {
        "indices.store.throttle.max_bytes_per_sec" : "100mb"
    }
}
5.0开始，es对此作了大幅度改进，使用了Lucene的CMS(ConcurrentMergeScheduler)的auto throttle机制，正常情况下已经不再需要手动配置 indices.store.throttle.max_bytes_per_sec 了。
官方文档中都已经删除了相关介绍，不过从源码中还是可以看到，这个值目前的默认设置是10240MB。

归并线程的数目
推荐设置为CPU核心数的一半，如果磁盘性能较差，可以适当降低配置，避免发生磁盘IO堵塞:
PUT employee/_settings
{
    "index.merge.scheduler.max_thread_count" : 8
}
5.0之后，归并线程的数目，ES也是有所控制的。默认数目的计算公式是：Math.min(3, Runtime.getRuntime().availableProcessors() / 2)。
即服务器CPU核数的一半大于3时，启动3个归并线程；否则启动跟CPU核数的一半相等的线程数。
相信一般做Elastic Stack的服务器CPU合数都会在6个以上。所以一般来说就是3个归并线程。
如果你确定自己磁盘性能跟不上，可以降低index.merge.scheduler.max_thread_count配置，免得IO情况更加恶化。

归并默认的最大segment大小是5GB，即如果一个segment超过了5GB，那么es就不再对它进行merge。
那么一个比较庞大的数据索引，就必然会有为数不少的大于5GB的segment永远存在，这对文件句柄，内存等资源都是极大的浪费。
但是由于归并任务太消耗资源，所以一般不太选择加大index.merge.policy.max_merged_segment配置，而是在负载较低的时间段，通过forcemerge接口，强制归并segment。
POST /ik_test/_forcemerge
{
  "max_num_segments":1
}

一个segment是一个完备的lucene倒排索引，而倒排索引是通过词典(Term Dictionary)到文档列表(Postings List)的映射关系，快速做查询的。
所以每个segment都有会一些索引数据驻留在heap里。因此segment越多，瓜分掉的heap也越多，并且这部分heap是无法被GC掉的。
所以我们应该尽量减少segment对内存的占用，有三种方法：
1.删除不用的索引。
2.关闭暂时不用的索引(文件仍然存在于磁盘，只是释放掉内存),需要的时候可重新打开。
3.定期对不再更新的索引做force merge(之前版本是optimze)，一般选择负载比较小的时间去手动force merge。

四、关于es JVM配置优化
Elasticsearch 默认安装后设置的堆内存是1GB。对于任何一个业务部署来说，这个设置都太小了。

1.把你的内存的一半(或少于)给Lucene
es是基于lucene实现的，es启动后，lucene会将磁盘的中所有segment加载到内存中，以便提高搜索的效率，所以你得分相当一部分的内存给lucene。

2.不要超过32GB
JVM在内存小于32GB的时候会采用一个内存对象指针压缩技术。
即便你有足够的内存，也尽量不要超过32GB。因为它浪费了内存，降低了CPU的性能，还要让GC应对大内存。
