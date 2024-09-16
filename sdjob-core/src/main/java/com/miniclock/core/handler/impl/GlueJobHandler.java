package com.miniclock.core.handler.impl;


import com.miniclock.core.context.SdJobHelper;
import com.miniclock.core.handler.IJobHandler;

public class GlueJobHandler extends IJobHandler {

	private long glueUpdatetime;
	private IJobHandler jobHandler;

	public GlueJobHandler(IJobHandler jobHandler, long glueUpdatetime) {
		this.jobHandler = jobHandler;
		this.glueUpdatetime = glueUpdatetime;
	}
	public long getGlueUpdatetime() {
		return glueUpdatetime;
	}


	@Override
	public void execute() throws Exception {
		SdJobHelper.log("----------- glue.version:"+ glueUpdatetime +" -----------");
		jobHandler.execute();
	}

	@Override
	public void init() throws Exception {
		this.jobHandler.init();
	}

	@Override
	public void destroy() throws Exception {
		this.jobHandler.destroy();
	}
}
