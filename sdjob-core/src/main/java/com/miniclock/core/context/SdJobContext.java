package com.miniclock.core.context;

/**
 * 定时任务上下文
 */
public class SdJobContext {

    public static final int HANDLE_CODE_SUCCESS = 200;
    public static final int HANDLE_CODE_FAIL = 500;
    public static final int HANDLE_CODE_TIMEOUT = 502;


    private final long jobId;


    private final String jobParam;


    private final String jobLogFileName;

    private int handleCode;


    private String handleMsg;


    public SdJobContext(long jobId, String jobParam, String jobLogFileName ) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.jobLogFileName = jobLogFileName;
        //构造方法中唯一值得注意的就是这里，创建XxlJobContext对象的时候默认定时任务的执行结果就是成功
        //如果执行失败了，自由其他方法把这里设置成失败
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

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    //这里是一个线程的本地变量，因为定时任务真正执行的时候，在执行器端是一个定时任务任务对应一个线程
    //这样就把定时任务隔离开了，自然就可以利用这个线程的本地变量，把需要的数据存储在里面
    //这里使用的这个变量是可继承的threadlocal，也就子线程可以访问父线程存储在本地的数据了
    private static InheritableThreadLocal<SdJobContext> contextHolder = new InheritableThreadLocal<SdJobContext>();


    public static void setXxlJobContext(SdJobContext sdJobContext){
        contextHolder.set(sdJobContext);
    }


    public static SdJobContext getInstance(){
        return contextHolder.get();
    }

}