package general;

import recvFrames.Listener;

public class flow_counter implements Runnable{
	void printf(String s) {
		System.out.print(s);
	}
	public void run() {
		while(true){
			//usleep(1000000);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			printf("上行流量：");
			if(Listener.flowAcc_upstream<1024){
				System.out.print(Listener.flowAcc_upstream+"B/s,");
			}
			else if(Listener.flowAcc_upstream<1048576){
				System.out.print(Listener.flowAcc_upstream/1024.0+"KB/s,");
			}
			else if(Listener.flowAcc_upstream<1073741824){
				System.out.print(Listener.flowAcc_upstream/1048576.0+"MB/s,");
			}
			else/* if(flowAcc_upstream<1073741824)*/{
				System.out.print(Listener.flowAcc_upstream/1073741824.0+"GB/s,");
			}
			printf("下行流量：");
			if(Listener.flowAcc_downstream<1024){
				System.out.println(Listener.flowAcc_downstream+"B/s");
			}
			else if(Listener.flowAcc_downstream<1048576){
				System.out.println(Listener.flowAcc_downstream/1024.0+"KB/s");
			}
			else if(Listener.flowAcc_downstream<1073741824){
				System.out.println(Listener.flowAcc_downstream/1048576.0+"MB/s");
			}
			else/* if(flowAcc_downstream<1073741824)*/{
				System.out.println(Listener.flowAcc_downstream/1073741824.0+"GB/s");
			}
			//flowAcc_upstream_last1s = flowAcc_upstream;
			//flowAcc_downstream_last1s = flowAcc_downstream;
			Listener.flowAcc_upstream = 0;
			Listener.flowAcc_downstream = 0;
		}
	}
}
