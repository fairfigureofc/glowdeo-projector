#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <poll.h>
typedef uint32_t u32;
struct cap {char driver[16],card[32],bus[32];u32 version,caps,dcaps,res[3];};
struct fmt {u32 type;u32 data[50];};
struct desc {u32 index,type,flags;char description[32];u32 fourcc,mbus,res[3];};
struct req {u32 count,type,memory,caps;unsigned char flags,res[3];};
struct buf {u32 index,type,bytesused,flags,field;int32_t sec,usec;unsigned char timecode[16];u32 sequence,memory,offset,length,reserved;int32_t request;};
#define QCAP _IOR('V',0,struct cap)
#define ENUM _IOWR('V',2,struct desc)
#define GFMT _IOWR('V',4,struct fmt)
#define REQ _IOWR('V',8,struct req)
#define QUERY _IOWR('V',9,struct buf)
#define QUEUE _IOWR('V',15,struct buf)
#define DEQUEUE _IOWR('V',17,struct buf)
#define ON _IOW('V',18,int)
#define OFF _IOW('V',19,int)
static void fail(const char*s){perror(s);exit(1);}
int main(int argc,char**argv){
 int fd=open("/dev/video0",O_RDWR|O_NONBLOCK);if(fd<0)fail("open");
 struct cap c={0};if(ioctl(fd,QCAP,&c))fail("querycap");printf("driver=%s card=%s caps=%08x device_caps=%08x\n",c.driver,c.card,c.caps,c.dcaps);
 for(u32 i=0;i<16;i++){struct desc d={.index=i,.type=1};if(ioctl(fd,ENUM,&d))break;printf("format %u %.4s %s\n",i,(char*)&d.fourcc,d.description);}
 struct fmt f={.type=1};if(ioctl(fd,GFMT,&f))fail("getfmt");printf("current width=%u height=%u fourcc=%.4s stride=%u size=%u\n",f.data[0],f.data[1],(char*)&f.data[2],f.data[4],f.data[5]);fflush(stdout);
 if(argc<2){close(fd);return 0;}
 struct req r={.count=3,.type=1,.memory=1};if(ioctl(fd,REQ,&r))fail("request buffers");if(r.count>8||!r.count)fail("buffer count");
 void* maps[8]={0};u32 lens[8]={0};
 for(u32 i=0;i<r.count;i++){struct buf b={.index=i,.type=1,.memory=1};if(ioctl(fd,QUERY,&b))fail("query buffer");lens[i]=b.length;maps[i]=mmap(0,b.length,PROT_READ|PROT_WRITE,MAP_SHARED,fd,b.offset);if(maps[i]==MAP_FAILED)fail("mmap");if(ioctl(fd,QUEUE,&b))fail("queue");}
 int type=1;if(ioctl(fd,ON,&type))fail("stream on");int ok=0;
 for(int n=0;n<6;n++){struct pollfd p={.fd=fd,.events=POLLIN};if(poll(&p,1,3000)<=0){fprintf(stderr,"camera timeout\n");break;}struct buf b={.type=1,.memory=1};if(ioctl(fd,DEQUEUE,&b)){perror("dequeue");break;}if(b.index>=r.count||b.bytesused>lens[b.index])break;
 if(n==5){FILE*out=fopen(argv[1],"wb");if(!out)break;ok=fwrite(maps[b.index],1,b.bytesused,out)==b.bytesused;fclose(out);printf("saved %u bytes to %s\n",b.bytesused,argv[1]);}
 if(ioctl(fd,QUEUE,&b))break;}
 ioctl(fd,OFF,&type);for(u32 i=0;i<r.count;i++)munmap(maps[i],lens[i]);r.count=0;ioctl(fd,REQ,&r);close(fd);return ok?0:1;
}
