#include "lrc_read.h"
#include <QFile>
#include <QDebug>
bool lrc_open()
{
    QFile file("/home/xavier/Desktop/music/1.lrc");
    if(file.exists()){
       qDebug()<<"open lrc ok";return true;
     }
     else{
       qDebug()<<"open lrc error";return false;
     }



}


