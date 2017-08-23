/*
 * Copyright (C) 2014 Bluetooth Connection Template
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.o3selab.smartlock.services;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.List;

import kr.o3selab.smartlock.common.AppSettings;

public class ServiceMonitoring {
	
	private static long SERVICE_RESTART_INTERVAL = 60*1000;
	

	/**
	 * 서비스가 실행되고있는지 확인
	 * @param context
	 * @param cls			name of service
	 * @return	boolean		is running or not
	 */
	private static boolean isRunningService(Context context, Class<?> cls) {
		boolean isRunning = false;

		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> info = activityManager.getRunningServices(Integer.MAX_VALUE);

		if (info != null) {
			for(ActivityManager.RunningServiceInfo serviceInfo : info) {
				ComponentName compName = serviceInfo.service;
				String className = compName.getClassName();

				if(className.equals(cls.getName())) {
					isRunning = true;
					break;
				}
			}
		}
		return isRunning;
	}
	
	/**
	 * 백그라운드 서비스 시작 SERVICE_RESTART_INTERVAL 주기마다 재시작.
	 * @param context
	 */
	public static void startMonitoring(Context context) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, ServiceMonitoringBR.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		am.setRepeating(AlarmManager.ELAPSED_REALTIME , SystemClock.elapsedRealtime(), SERVICE_RESTART_INTERVAL, pi);
	}

	/**
	 * 백그라운드 서비스 종료
	 * @param context
	 */
	public static void stopMonitoring(Context context) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, ServiceMonitoringBR.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		am.cancel(pi);
	}
	
	
	/**
	 *	브로드 캐스트 수신
	 */
	public static class ServiceMonitoringBR extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Logs.d("# Monitoring service");
			
			//백그라운드 체크 확인 설정 값
				AppSettings.initializeAppSettings(context);
				if(!AppSettings.getBgService()) {
					stopMonitoring(context);
					return;
			}
			// 서비스가 실행중이 아닌 경우, 서비스를 시작.
			if(isRunningService(context, BTCTemplateService.class) == false) {
				/*Intent intent1 = new Intent("com.god.smartlook.service.BTCTemplateService");
				context.startService(intent1);*/

                context.startService(new Intent(context, BTCTemplateService.class));

			}
		}
	}
	
}
