package com.dianping.puma.remote.reporter;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dianping.lion.client.ConfigCache;
import com.dianping.lion.client.ConfigChange;
import com.dianping.lion.client.LionException;
import com.dianping.puma.core.util.ScheduledExecutorUtils;
import com.dianping.puma.remote.reporter.helper.ServerAckService;

@Component("serverAckScheduledService")
public class ServerAckScheduledService {

	private static final Logger LOG = LoggerFactory.getLogger(ServerAckScheduledService.class);

	private static final String SERVER_ACK_INTERVAL = "puma.server.serverack.interval";

	private static final String FACTORY_NAME = "serverAck";

	@Autowired
	private ServerAckService serverAckService;

	private volatile long interval = 5000L;

	private ScheduledExecutorService executorService = null;

	private ScheduledFuture scheduledFuture;

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public ScheduledFuture getScheduledFuture() {
		return this.scheduledFuture;
	}

	public void setScheduledFuture(ScheduledFuture scheduledFuture) {
		this.scheduledFuture = scheduledFuture;
	}

	public void setScheduledExecutorService(ScheduledExecutorService executorService) {
		this.executorService = executorService;
	}

	public ScheduledExecutorService getScheduledExecutorService() {
		return this.executorService;
	}

	private boolean isScheduledFutureValid() {
		if (getScheduledFuture() != null && !getScheduledFuture().isCancelled()) {
			return true;
		}
		return false;
	}

	private boolean isExecutorServiceValid() {
		if (getScheduledExecutorService() != null && !getScheduledExecutorService().isShutdown()) {
			return true;
		}
		return false;
	}

	public ServerAckScheduledService() {
		initConfig();

		executorService = ScheduledExecutorUtils.createSingleScheduledExecutorService(FACTORY_NAME);

		execute();
	}

	public void initConfig() {
		try {
			this.setInterval(ConfigCache.getInstance().getLongProperty(SERVER_ACK_INTERVAL));
			ConfigCache.getInstance().addChange(new ConfigChange() {
				@Override
				public void onChange(String key, String value) {
					if (SERVER_ACK_INTERVAL.equals(key)) {
						setInterval(Long.parseLong(value));
						if (isScheduledFutureValid()) {
							getScheduledFuture().cancel(true);
							if (isExecutorServiceValid()) {
								execute();
							}
						}
					}
				}
			});
		} catch (LionException e) {
			LOG.error(SERVER_ACK_INTERVAL +" Lion config read exception, Reason: ", e.getMessage());
		}
	}

	public void execute() {
		scheduledFuture = executorService.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				serverAckService.pushServerAcks();
			}
		}, 0, getInterval(), TimeUnit.MILLISECONDS);
	}

}