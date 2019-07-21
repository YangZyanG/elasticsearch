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