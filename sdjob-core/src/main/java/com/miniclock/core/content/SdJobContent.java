package com.miniclock.core.content;

/**
 * @author strind
 * @date 2024/8/24 19:12
 * @description 定时任务的上下文
 */
public class SdJobContent {

    public static final int HANDLE_CODE_SUCCESS = 200;
    public static final int HANDLE_CODE_FAIL = 500;
    public static final int HANDLE_CODE_TIMEOUT = 502;


    private final long jobId;


    private final String jobParam;


    private final String jobLogFileName;


    private final int shardIndex;


    private final int shardTotal;


    private int handleCode;


    private String handleMsg;


    public SdJobContent(long jobId, String jobParam, String jobLogFileName, int shardIndex, int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.jobLogFileName = jobLogFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
        this.handleCode = HANDLE_CODE_SUCCESS;
    }

    public long getJobId() {
        return jobId;
    }

    public String getJobParam() {
        return jobParam;
    }

    public String getJobLogFileName() {
        return jobLogFileName;
    }

    public int getShardIndex() {
        return shardIndex;
    }

    public int getShardTotal() {
        return shardTotal;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    //这里是一个线程的本地变量，因为定时任务真正执行的时候，在执行器端是一个定时任务任务对应一个线程
    //这样就把定时任务隔离开了，自然就可以利用这个线程的本地变量，把需要的数据存储在里面
    //这里使用的这个变量是可继承的threadlocal，也就子线程可以访问父线程存储在本地的数据了
    private static InheritableThreadLocal<SdJobContent> contextHolder = new InheritableThreadLocal<>();


    public static void setXxlJobContext(SdJobContent sdJobContent){
        contextHolder.set(sdJobContent);
    }


    public static SdJobContent getXxlJobContext(){
        return contextHolder.get();
    }
}
