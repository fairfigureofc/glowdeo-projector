#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <signal.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <netinet/in.h>
#include <time.h>
#include "key.h"
static char state[4096]="{\"mode\":\"scene\",\"revision\":0}";
static char capture[2048];
static int sendall(int s,const void*p,size_t n){while(n){ssize_t k=send(s,p,n,0);if(k<=0)return -1;p=(const char*)p+k;n-=k;}return 0;}
static void head(int s,int code,const char*type,long n){char b[1024];int k=snprintf(b,sizeof b,"HTTP/1.1 %d OK\r\nContent-Type: %s\r\nContent-Length: %ld\r\nAccess-Control-Allow-Origin: *\r\nAccess-Control-Allow-Headers: Content-Type, Authorization\r\nAccess-Control-Allow-Methods: GET, POST, OPTIONS\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n",code,type,n);sendall(s,b,k);}
static void msg(int s,int code,const char*b){head(s,code,"application/json",strlen(b));sendall(s,b,strlen(b));}
static void fileout(int s,const char*name,const char*type){int fd=open(name,O_RDONLY);if(fd<0){msg(s,404,"{\"error\":\"missing\"}");return;}struct stat st;fstat(fd,&st);head(s,200,type,st.st_size);char buf[16384];ssize_t n;while((n=read(fd,buf,sizeof buf))>0)if(sendall(s,buf,n))break;close(fd);}
int main(int argc,char**argv){if(argc<3)return 1;chdir(argv[1]);snprintf(capture,sizeof capture,"%s/libcapture.so",argv[2]);signal(SIGPIPE,SIG_IGN);int fd=socket(AF_INET,SOCK_STREAM,0),one=1;setsockopt(fd,SOL_SOCKET,SO_REUSEADDR,&one,sizeof one);struct sockaddr_in addr={.sin_family=AF_INET,.sin_port=htons(8787),.sin_addr.s_addr=htonl(INADDR_LOOPBACK)};if(bind(fd,(void*)&addr,sizeof addr)||listen(fd,8))return 2;
for(;;){int s=accept(fd,0,0);if(s<0)continue;struct timeval timeout={.tv_sec=8};setsockopt(s,SOL_SOCKET,SO_RCVTIMEO,&timeout,sizeof timeout);setsockopt(s,SOL_SOCKET,SO_SNDTIMEO,&timeout,sizeof timeout);
char h[16384];size_t used=0;char*end=0;while(used<sizeof(h)-1){ssize_t n=recv(s,h+used,sizeof(h)-1-used,0);if(n<=0)break;used+=n;h[used]=0;end=strstr(h,"\r\n\r\n");if(end)break;}if(!end){close(s);continue;}char method[16],path[2048];sscanf(h,"%15s %2047s",method,path);
if(!strcmp(method,"OPTIONS")){msg(s,200,"{}");close(s);continue;}
char auth[128];snprintf(auth,sizeof auth,"key=%s",CONTROL_KEY);char bearer[128];snprintf(bearer,sizeof bearer,"Bearer %s",CONTROL_KEY);if(!strstr(path,auth)&&!strstr(h,bearer)){msg(s,403,"{\"error\":\"unauthorized\"}");close(s);continue;}
char*q=strchr(path,'?');if(q)*q=0;long len=0;char*cl=strcasestr(h,"Content-Length:");if(cl)len=strtol(cl+15,0,10);if(len<0||len>20000000){msg(s,413,"{}");close(s);continue;}char*body=calloc(1,len+1);size_t initial=used-(end+4-h);if(initial>(size_t)len)initial=len;memcpy(body,end+4,initial);size_t total=initial;while(total<(size_t)len){ssize_t n=recv(s,body+total,len-total,0);if(n<=0)break;total+=n;}if(total!=(size_t)len){free(body);close(s);continue;}
if(!strcmp(path,"/health")){char b[128];snprintf(b,sizeof b,"{\"ok\":true,\"uid\":%d,\"transport\":\"direct-wifi\"}",getuid());msg(s,200,b);}
else if(!strcmp(path,"/state"))msg(s,200,state);
else if(!strcmp(path,"/command")&&!strcmp(method,"POST")&&len<sizeof(state)){memcpy(state,body,len);state[len]=0;msg(s,200,"{\"ok\":true}");}
else if(!strcmp(path,"/image")&&!strcmp(method,"POST")){FILE*f=fopen("uploaded-image.tmp","wb");if(f){fwrite(body,1,len,f);fclose(f);rename("uploaded-image.tmp","uploaded-image");msg(s,200,"{\"ok\":true}");}else msg(s,500,"{}");}
else if(!strcmp(path,"/image"))fileout(s,"uploaded-image","application/octet-stream");
else if(!strcmp(path,"/capture")&&!strcmp(method,"POST")){pid_t child=fork();if(!child){int null=open("/dev/null",O_WRONLY);dup2(null,1);dup2(null,2);execl(capture,capture,"camera.yuyv",(char*)0);_exit(127);}int status=0;waitpid(child,&status,0);if(WIFEXITED(status)&&WEXITSTATUS(status)==0)fileout(s,"camera.yuyv","application/octet-stream");else{char b[160];snprintf(b,sizeof b,"{\"error\":\"camera capture failed\",\"status\":%d,\"uid\":%d}",status,getuid());msg(s,500,b);}}
else msg(s,404,"{}");free(body);close(s);}}
