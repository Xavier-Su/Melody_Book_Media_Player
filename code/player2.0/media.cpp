
#include "media.h"
#include "ui_media.h"
#include<QPushButton>
#include<qdebug.h>
#include "lrc_read.h"
#include <QFile>
#include <QDebug>

static QMap<QString, QString> map_lrc;

Media::Media(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::Media)
{

    ui->setupUi(this);
    setWindowTitle("媒体播放器");
    QPalette pal =this->palette();
    pal.setBrush(QPalette::Background,QBrush(QPixmap(QCoreApplication::applicationDirPath()+"/bj1.jpg")));//背景图片
    setPalette(pal);

    pause=false;
    playProcess =new QProcess(this);
    connect(playProcess,SIGNAL(readyReadStandardOutput()),this,SLOT(redate()));


}
 static  QString notice="Which media will play";
 static  QString path="/media";
 static  QString type="video(*.mp4 *.avi *.mp3)";

Media::~Media()
{
    playProcess->kill();
    delete ui;
}

void Media::on_Add_clicked()
{
    this->update();
    //playProcess->kill();
    //QStringList mediafile = QFileDialog::getOpenFileNames(this,"Which media will play","/home","video(*.mp4 *.wmv *.avi *.mp3)");
    QStringList mediafile = QFileDialog::getOpenFileNames(this,notice,path,type);
    if(mediafile.length()<=0){
        return;
    }

    if(mediafile.count()!=0){
//        mediafile.at(0);
//        QString name="2222";
        ui->listWidget->addItems(mediafile);
//        qDebug()<<"addItems"<<mediafile;
    }

//    play(mediafile.at(0));
    this->update();
}




void Media::play(QString filename){
    this->playProcess->kill();
    if(!playProcess->waitForFinished(3000)){qDebug()<<"waitForFinished";}
    ui->lrc_now->clear();
    ui->lrc_next->clear();

    QMap<QString, QString>::iterator iter = map_lrc.begin();
    while (iter != map_lrc.end())
    {
        map_lrc.remove(iter.key());
        iter++;
    }

    if(1){


        QFileInfo fileInfo = QFileInfo(filename);

        QString fileSuffix = fileInfo.suffix();
        QString lrc_name=filename;
        lrc_name.replace(fileSuffix,"lrc");
//        qDebug()<<" lrc "<<lrc_name;



        QFile file(lrc_name);
        if(file.exists()){
           qDebug()<<"open lrc ok";
         }
         else{
           qDebug()<<"open lrc error";
           ui->lrc_now->setText("暂无歌词");
           ui->lrc_next->setText("");
         }
        file.open(QIODevice::ReadOnly | QIODevice::Text);
        QString line="";
        while((line=file.readLine())>0){
            QRegExp re("[0-9]{2}:[0-9]{2}");
            if(line.indexOf(re) >= 0)
              {
                QString lrc_time= re.cap(0);
                QString lrc_text=line.section(QRegExp("[]\s^\n]"),1,1);

                map_lrc.insert(lrc_time, lrc_text);
              }
        }
    }




    QString program =QCoreApplication::applicationDirPath()+"\\mplayer\\mplayer.exe";
    QStringList commond;
    commond << filename;
    commond << "-zoom";
    commond << "-x";
    commond << "1024";
    commond << "-y";
    commond << "600";
    commond << "-slave";//从模式
    commond << "-quiet";//静默
    QFileInfo fileInfo = QFileInfo(filename);
    QString media_name = fileInfo.fileName();
//    qDebug()<<"54848"<<media_name;

//    QRegExp re("([\^<>/\\\\|:""\\*\\?]+)\\.\\w+$");

    QRegExp re("[^/.\w+.]+");
    if(media_name.indexOf(re) >= 0)
      {
        media_name= re.cap(0);
      }

    ui->medianow->setText("正在播放: "+media_name);

    this->playProcess->start(program,commond);


}

void Media::on_listWidget_itemDoubleClicked(QListWidgetItem *item)
{
    this->playProcess->kill();
    play(item->text());
}

void Media::on_delete_2_clicked()
{
    int index = ui->listWidget->currentRow();
    ui->listWidget->takeItem(index);
    this->playProcess->kill();
}


void Media::on_Pause_clicked()
{
    pause=!pause;
    this->playProcess->write("pause\n");
    if(pause){
        disconnect(playProcess,SIGNAL(readyReadStandardOutput()),this,SLOT(redate()));
    }
    if(!pause){
        connect(playProcess,SIGNAL(readyReadStandardOutput()),this,SLOT(redate()));
        playProcess->write("get_time_length\n");//发命令，cmd命令
        playProcess->write("get_time_pos\n");
        playProcess->write("get_percent_pos\n");
    }




}


void Media::on_Forward_clicked()
{
    this->playProcess->write("seek +10 0\n");

}
void Media::on_next_song_clicked()
{
        int row = ui->listWidget->currentRow();
        int count=ui->listWidget->count();
        if (row>count-2){ui->listWidget->setCurrentRow(0);}
        else{ui->listWidget->setCurrentRow(row+1);}
//        qDebug()<<"row="<<row<<"count"<<count;

        QString item = ui->listWidget->currentItem()->text();

        play(item);
//      qDebug()<<"row="<<row<<item;
}

void Media::on_Backward_clicked()
{
    this->playProcess->write("seek -5 0\n");
}

void Media::on_previous_song_clicked()
{
    int row = ui->listWidget->currentRow();
    int count=ui->listWidget->count();
    if (row==0){ui->listWidget->setCurrentRow(count-1);}
    else{ui->listWidget->setCurrentRow(row-1);}
    QString item = ui->listWidget->currentItem()->text();
    play(item);
}


void Media::on_volumeup_clicked()
{
    this->playProcess->write("volume +5\n");
}


void Media::on_volumedown_clicked()
{
    this->playProcess->write("volume -5\n");
}

void Media::on_Exit_clicked()
{
    //关闭
    this->update();
    this->playProcess->kill();
    this->close();
}


void Media::redate(){

    this->playProcess->write("get_time_length\n");
    this->playProcess->write("get_time_pos\n");
    this->playProcess->write("get_percent_pos\n");

    while(this->playProcess->canReadLine())
    {
        QByteArray order=this->playProcess->readLine();
//         qDebug()<<"order"<<order;
        if(order.startsWith("ANS_TIME_POSITION"))
        {
           order.replace(QByteArray("\n"),QByteArray(""));
           QString content(order);
           Str=content.mid(18).simplified();
//           qDebug()<<"Str"<<Str;
           int tim=qFloor(Str.toDouble());
           ui->horizontalSlider->setValue(tim);
        }
        else if((order.startsWith("ANS_LENGTH")))
        {
           order.replace(QByteArray("\n"),QByteArray(""));
           QString content(order);
           Time=content.mid(11).simplified();
           QString mins=QString::number(int(Str.toDouble())/60,10);QString secs=QString::number(int(Str.toDouble())%60,10);
           //qDebug()<<mins<<":"<<secs<<endl;

           QString min2s=QString::number(int(Time.toDouble())/60,10);QString sec2s=QString::number(int(Time.toDouble())%60,10);
           //qDebug()<<min2s<<":"<<sec2s<<endl;
           QString timetips="";
           if (secs.toInt()<10){
               timetips="0"+mins+":"+"0"+secs;
           }else {
               timetips="0"+mins+":"+secs;
           }


           QString time_lrc=map_lrc.value(timetips);
//           ui->lrc_now->setText(time_lrc);

           QMap<QString, QString>::iterator iter = map_lrc.begin();
           while (iter != map_lrc.end())
           {
               if(iter.key()==timetips){ui->lrc_now->setText(time_lrc);
                ui->lrc_next->setText(map_lrc.value((iter+1).key()));}
               iter++;
           }


//           qDebug()<<timetips<<":"<<time_lrc<<endl;

           ui->timetext->setText(mins+":"+secs);
           ui->timetext_2->setText(min2s+":"+sec2s);
           if((mins.toInt()*60+secs.toInt())==(min2s.toInt()*60+sec2s.toInt()-2)){on_next_song_clicked();}

           ui->horizontalSlider->setRange(0,qFloor(Time.toDouble()));
        }

    }

}

//  Esc关闭播放窗口
//  Space 播放，暂停
//  Shift打开添加按钮
//  L快进
//  J快退
//  K音量小
//  I音量大

void Media::keyPressEvent(QKeyEvent *event){
    switch(event->key()){
    case Qt::Key_Shift://添加
        on_Add_clicked();
        break;
    case Qt::Key_B://播放，暂停
        on_Pause_clicked();
        break;
    case Qt::Key_Escape://关闭播放窗口
        on_Exit_clicked();
        break;
    case Qt::Key_L://快进
        on_Forward_clicked();
        break;
    case Qt::Key_J://快退
        on_Backward_clicked();
        break;
    case Qt::Key_K://音量小
        on_volumedown_clicked();
        break;
    case Qt::Key_I://音量大
        on_volumeup_clicked();
        break;
    case Qt::Key_O:
           ui->horizontalSlider->hide();
           ui->timetext->hide();
           ui->timetext_2->hide();
           break;
       case Qt::Key_U:
           ui->horizontalSlider->show();
           ui->timetext->show();
           ui->timetext_2->hide();
           break;

    }
}




static int tips=0;
void Media::on_horizontalSlider_sliderMoved(int position)
{
    tips=position;
//        qDebug()<<QString::number(tips)<<":"<<QString::number(position)<<endl;

}

void Media::on_horizontalSlider_sliderReleased()
{

        this->playProcess->write(QString("seek "+QString::number(tips)+" 2\n").toUtf8());
        on_Pause_clicked();
//        qDebug()<<"Released"<<QString::number(tips)<<endl;
}

void Media::on_horizontalSlider_valueChanged(int value)
{
    ui->horizontalSlider->setTracking(false);
//    if(ui->horizontalSlider->value()!=value)
//    {this->playProcess->write(QString("seek "+QString::number(value)+" 2\n").toUtf8());}
//    qDebug() << "ui->horizontalSlider->value()" << ui->horizontalSlider->value();
//    qDebug() << "valueChanged" << value;

}

void Media::on_horizontalSlider_sliderPressed()
{           on_Pause_clicked();
            qDebug()<<"Pressed"<<QString::number(ui->horizontalSlider->value())<<endl;
}



static bool list_if=true;
void Media::on_list_button_clicked()
{
    list_if=!list_if;
    if(!list_if)
    {
        ui->listWidget->hide();
        }
    else {
        ui->listWidget->show();
}

}
