
// 编译：gcc -o (目标可执行文件名) (本源代码文件名) -lpcap -lpthread
// 执行：sudo ./(目标可执行文件名) (网卡名-代表要监听的网卡，实验中需要选择无线网卡，默认会听到ifconfig里显示的第一个网卡)
#include "general.h"

#define SIZE 1601
#define STDIN 0
#define MAX_DATA SIZE
/* 全局变量区 */
//监听键盘事件的变量（按q键退出）
int keyboard_stop = 0;
//监控流量的变量（单位字节）
int flowAcc_upstream = 0;
int flowAcc_downstream = 0;
int flowAcc_upstream_last1s = 0;
int flowAcc_downstream_last1s = 0;
long total_streambytes = 0;
//后台处理节点参数
char *cserver = "192.168.0.11";
uint32_t PORT = 9036;

//当前视频流相关数据


/* 函数区 */
/* 10转16进制 x=10进制数（0-255），res=16进制字符串 */
void DecToHex(int x,char res[3]){
	int high=x/16,low=x%16;
	if(high<=9)res[0]=(char)(high+48);
	else res[0]=(char)(high+87);
	if(low<=9)res[1]=(char)(low+48);
	else res[1]=(char)(low+87);
	res[2]=0;
}

char hex(int x){
	switch(x){
		case 0: return '0';
		case 1: return '1';
		case 2: return '2';
		case 3: return '3';
		case 4: return '4';
		case 5: return '5';
		case 6: return '6';
		case 7: return '7';
		case 8: return '8';
		case 9: return '9';
		case 10:return 'A';
		case 11:return 'B';
		case 12:return 'C';
		case 13:return 'D';
		case 14:return 'E';
		case 15:return 'F';
		default:return '\0';
	}
}

void DecToHex_upg(int x, char *res){
	int digits=0,d[4]={0};
	while(x>0){
		d[digits++]=x%16;
		x/=16;
	}
	res[0]=hex(d[3]);
	res[1]=hex(d[2]);
	res[2]=hex(d[1]);
	res[3]=hex(d[0]);
	res[4]='\0';
}
/* 终止抓包线程侦听（按下q键） */
void stop_listener( void *ptr ) {
    struct timeval tv = {0,0};
    struct termios term, termbak;
    char ch;
    fd_set fd;

    FD_ZERO(&fd);
    FD_SET( STDIN ,&fd);
 
    tcgetattr(STDIN, &term);
    termbak = term;
    term.c_lflag &= ~(ICANON|ECHO);
    tcsetattr(STDIN, TCSANOW, &term); 
 
    while(1)
    {
        FD_ZERO(&fd);
        FD_SET( STDIN ,&fd);
        if(1 == select( STDIN+1,&fd,NULL,NULL,&tv)  && 1 == read( STDIN , &ch , 1 ) && 'q' == tolower(ch) )
	{
		keyboard_stop=1;
	        break;
	}
        fflush(stdout);
        usleep(100000);
    }
     
    tcsetattr(STDIN,TCSANOW,&termbak);
}



/* 统计流量的线程 */
void flowAccount( void *ptr ){
	while(keyboard_stop==0){
		usleep(1000000);
		printf("上行流量：");
		if(flowAcc_upstream<1024){
			printf("%dB/s，",flowAcc_upstream);
		}
		else if(flowAcc_upstream<1048576){
			printf("%.2fKB/s，",(double)flowAcc_upstream/1024.0);
		}
		else if(flowAcc_upstream<1073741824){
			printf("%.2fMB/s，",(double)flowAcc_upstream/1048576.0);
		}
		else/* if(flowAcc_upstream<1073741824)*/{
			printf("%.2fGB/s，",(double)flowAcc_upstream/1073741824.0);
		}
		printf("下行流量：");
		if(flowAcc_downstream<1024){
			printf("%dB/s,",flowAcc_downstream);
		}
		else if(flowAcc_downstream<1048576){
			printf("%.2fKB/s,",(double)flowAcc_downstream/1024.0);
		}
		else if(flowAcc_downstream<1073741824){
			printf("%.2fMB/s,",(double)flowAcc_downstream/1048576.0);
		}
		else/* if(flowAcc_downstream<1073741824)*/{
			printf("%.2fGB/s,",(double)flowAcc_downstream/1073741824.0);
		}
		printf("total bytes: %ld\n", total_streambytes);
		flowAcc_upstream_last1s = flowAcc_upstream;
		flowAcc_downstream_last1s = flowAcc_downstream;
		flowAcc_upstream = 0;
		flowAcc_downstream = 0;
	}
}

/* Key：发包到后台 */
int my_write(int fd, const void *buffer,int length, FILE *f){
	//写socket，要保证全部写完，返回正数表示写入了这么多字节，返回-1表示连接中断
	//参数：fd是socket描述符（在前面由socket(xxx)定义），buffer是待发送数据包（没加头的），length是buffer里待发送的长度
	//f测试用，在本地文件f中打印待发送的包（包括新加的包头和数据段），上线时写成NULL就行
	//（socket的发送缓冲区是400KB，如果缓冲区已经缓存这么大的数据仍没被取走的话将不再发送数据，因此需要协调好接收端以避免这种“假死”现象）
#define headLen 8
	int bytes_left=0;
	int written_bytes=0;
	char *ptr;


	//重新计算要发送的包（加上新包头3字节）
	char c_len[SIZE+headLen]={0};
	c_len[3]=(char)1;				//假设首位为1代表以太帧
	c_len[6]=(char)(length/256);
	c_len[7]=(char)(length%256);
	memcpy(c_len+headLen, buffer, length);
	
	//打印一下这个包（测试用）
	if(f!=NULL){					//珍爱电脑，远离Segmentation Fault
		if(f!=NULL)fprintf(f, "将要发给后台的帧格式(10进制）：\n");
		for(int i=0;i<length+headLen;i++){
			fprintf(f, "%d," , (int)(c_len[i]>=0?c_len[i]:256+c_len[i]));
		}
		if(f!=NULL)fprintf(f, "结束\n");
	}

	ptr=c_len;
	bytes_left=length+headLen;
	if(f!=NULL)fprintf(f, "准备发包，预计发送字节数%d\n", bytes_left);
	while(bytes_left>0)
	{
		 //written_bytes=write(fd,ptr,bytes_left);
		 written_bytes=send(fd, ptr, bytes_left, 0);
		 //written_bytes=bytes_left;			
		 if(f!=NULL)fprintf(f, "本次写入：%d字节\n",written_bytes);
		 if(written_bytes<=0)
		 {       
		        if(errno==EINTR)
			{
				printf("反复出现这句话的话可能出现了无法写socket问题，并进入死循环\n");
		                written_bytes=0;
			}
		        else             
		                 return -1;
		 }
		 bytes_left-=written_bytes;
		 ptr+=written_bytes;     
	}
	if(f!=NULL)fprintf(f, "该帧已写入：%d字节\n", length+headLen-bytes_left);
	total_streambytes += length+headLen-bytes_left;
	return length-bytes_left;
#undef headLen
}

int main(int argc, char **argv){
	/* 前置提示 */
	printf("命令: ./exe [设备名] [-o] [输出文件名] [-toHexString]\n");
	printf("带-o 参数表示观测所抓的包，并保存到输出文件名表示的文件处，不带表示不测试");
	printf("-toHexString 参数给文件的输出是十六进制的形式 (比如 \"01 02 03 ……\")\n");
	printf("否则输出的是报文的Ascii Code形式（除了HTTP，其他的是一团乱码）\n");
	/* 是否打印辅助信息（包的内容等） */
	FILE *outputFile = NULL;		//不打印任何辅助信息
	//outputFile = stdout;			//去除该行注释，将包的部分信息直接打印到命令行上
	if(argc>=4 && strcmp(argv[2],"-o")==0){
		//命令行给出了-o参数，这些信息打印到指定文件
		outputFile = fopen(argv[3], "wb");
		if(!outputFile){
			printf("error: 输出文件不存在或创建失败，程序将退出\n");
			exit(1);
		}
	}
	/*获取设备*/
	char errBuf[PCAP_ERRBUF_SIZE], *devStr;
	devStr = pcap_lookupdev(errBuf);
	if(devStr)
	{
		/*检测到网卡设备，返回success*/
		printf("检测到网卡设备: %s\n", devStr);
	}
	else
	{
		/*未检测到设备，返回error*/
		printf("没有检测到网卡: %s\n", errBuf);
		exit(1);
	}

	/* 监听设备 */
	pcap_t * device = pcap_open_live(devStr, 65535, 1, 0, errBuf);
	
	if(!device)
	{
		printf("pcap_open_live()函数错误: %s\n", errBuf);
		exit(1);
	}
	/* 线程检测键盘按钮事件（按q键退出） */
	pthread_t thread1;
	char *_p="";
	int ret_thrd1 = pthread_create(&thread1, NULL, (void *)&stop_listener, (void *)_p);
	if(!ret_thrd1){
		perror("键盘监听成功");
	}
	else printf("键盘监听初始化失败\n");
	/* 线程监控流量 */
	pthread_t thread_flowDetect;
	char *_p_flow="";
	int ret_thrd_flow = pthread_create(&thread_flowDetect, NULL, (void *)&flowAccount, (void *)_p_flow);
	if(!ret_thrd_flow){
		perror("流量监控");
	}
	else printf("流量监控初始化失败\n");
	/* 连接后台服务器 */
	int sockfd, new_fd;
	struct sockaddr_in dest_addr;
	char buf[MAX_DATA];

	sockfd = socket(AF_INET, SOCK_STREAM, 0);
	if(sockfd==-1){
		printf("socket连接失败，代码 %d", errno);
	}
	dest_addr.sin_family = AF_INET;
	dest_addr.sin_port=htons(PORT);
	dest_addr.sin_addr.s_addr=inet_addr(cserver);
	bzero(&(dest_addr.sin_zero), 8);

	if(connect(sockfd, (struct sockaddr*)&dest_addr, sizeof(struct sockaddr))==-1){
		printf("连接到后台服务器失败（一般是智障没启动后台或者后台端口改了造成Connection refused（错误代码111））实际错误代码：%d\n", errno);
	}else{
		printf("连接到后台服务器成功\n");
	}
	/* 抓包环节 */
	struct pcap_pkthdr packet;
	bpf_u_int32 net;
	bpf_u_int32 mask;
	pcap_lookupnet(devStr, &net, &mask, errBuf);
	int pack_count=0;
	char tmp[SIZE] = {0};
	while(keyboard_stop==0){
		/* 反复抓包 */
		if(outputFile!=NULL)fprintf(outputFile, "包ID:%d\n",pack_count);	//打印第几个包
		const u_char * pktStr = pcap_next(device, &packet);		
		/* 抓不到包（这种情况一般是路由器自己网断了） */
		if(!pktStr)
		{
			printf("没抓到包（检测一下路由器自己有没有联网或者路由器自己网断了？）\n");
			exit(1);
		}
		pack_count++;
		/* 抓到了一个包，此时pktStr的内容是包含包头的全部内容（就是WireShark点击包就看到的全部字节内容，分析数据需要筛掉包头 */
		// 给文件输出这个包
		/*
		if(argc>=5 && strcmp(argv[4],"-toHexString")==0)
		{
			char HexTransferBuff[3]={0};
			for(int charCount=0;charCount<=packet.caplen;charCount++){
				DecToHex(pktStr[charCount], HexTransferBuff);
				if(outputFile!=NULL)fprintf(outputFile, "%s ", HexTransferBuff);
			}
			if(outputFile!=NULL)fprintf(outputFile, "\n");
		}
		else{
			for(int charCount=0;charCount<=packet.caplen;charCount++){
				fputc(pktStr[charCount], outputFile);
			}
			if(outputFile!=NULL)fprintf(outputFile, "\n");
		}
		*/
		int packHeadPtr=0;		//记录包头字节数
		// 1.筛掉链路层头（14字节，包含源mac，目标mac，协议类型（一般是IPv4））
		// TODO: 如果对mac地址有什么操作可以在这里写
		if(pktStr[0]==0x00 && pktStr[1]==0x24)flowAcc_upstream+=packet.caplen;	//统计上下行流量。上线时条件需要更改
		else flowAcc_downstream+=packet.caplen;
		packHeadPtr+=14;
		// 2.筛掉IP头（一般是20字节，根据第一字节的一截而定）
		int ipFrag_bytes=((int)pktStr[packHeadPtr] & 0x0F)<<2;	//IP头的字节数，是第一字节的低4位×4
		if((int)pktStr[packHeadPtr+9]!=6){
			if(outputFile!=NULL)fprintf(outputFile, "这不是一个TCP包\n");
			continue;		//该字段位6代表TCP，否则不是TCP，不进入分析（11为UDP）
		}
		// TODO: 对IP头有什么操作可以写在这里
		packHeadPtr+=ipFrag_bytes;
		// 3. 筛掉TCP头（不定长，其长度根据某字节的一截而定）
		int tcpFrag_bytes=((int)pktStr[packHeadPtr+12] & 0xF0)>>2;	//TCP头的字节数，是第13字节的高4位×4
		// TODO: 对TCP头有什么操作可以写在这里
		packHeadPtr+=tcpFrag_bytes;
		// 4。剩下的就是TCP数据段
		char t_tmp[3];
		// TODO：这里写对数据段要干什么
		// （可选）匹配一下是否属于视频流（已经验证用哔哩哔哩、优酷、腾讯视频、腾讯体育的视频部分、百度贴吧视频贴、抖音观看视频时能够检测到。爱奇艺不行）
		if(strncmp(pktStr+packHeadPtr, "HTTP/1.1 206 Partial Content", strlen("HTTP/1.1 206 Partial Content"))==0){
			printf("发现HTTP 206包\n");
			//已经定位可能是视频流的包（下载包），继续判断是否视频流。这是正在使用视频软件并且正在App内观看视频的流（不是缓存）
			int typeFlagPtr=0;						//类型flag字段标志（Content-Type:video/*)
			while(strncmp(pktStr+typeFlagPtr, "Content-Type: video/", strlen("Content-Type: video/"))!=0 && typeFlagPtr<packet.caplen-strlen("Content-Type: video/")-1)typeFlagPtr++;
			if(typeFlagPtr>=packet.caplen-strlen("Content-Type: video/")-1){
				//不是视频流
				//Do nothing
			}
			else{
				//是视频流
				printf("检测到有终端正在看视频！\n");
				// TODO： 视频流的后续操作（比如给前端返回一个状态码）
				
			}
		}
		//转包给服务器
		memcpy(tmp, pktStr, packet.caplen);
		int send_bytes = my_write(sockfd, tmp, packet.caplen, outputFile);		//该行（my_write函数）目前会出现stack smashing detected错误，排查中……单机测试其他代码时把该行注释
		if(send_bytes==packet.caplen)if(outputFile!=NULL)fprintf(outputFile, "该包已完整发送\n");
		else if(send_bytes==-1)if(outputFile!=NULL)fprintf(outputFile, "发包：网络连接出错\n");
		else if(outputFile!=NULL)fprintf(outputFile, "发包不完整\n");
		if(pack_count%1000==0)printf("1000个包过去了\n");
	}
	printf("检测到键盘事件，退出\n");
	if(outputFile!=NULL)fclose(outputFile);
	pcap_close(device);
	close(sockfd);
}
