
#include "media.h"
#include "ui_media.h"
#include<QPushButton>
#include<qdebug.h>
#include "lrc_read.h"
#include <QFile>
#include <QDebug>

#include <QTimer>

static QMap<QString, QString> map_lrc,map_lrc_time,map_song_path;
static bool  pause=false;
static QTimer *timer;
static QTimer *Timer;
static int lrc_mid=6;
Media::Media(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::Media)
{

    ui->setupUi(this);
    setWindowTitle("音书v1.0  -by素白");
    this->setWindowIcon(QIcon(":/img/logo.png"));
    QPalette pal =this->palette();
    pal.setBrush(QPalette::Background,QBrush(QPixmap(QCoreApplication::applicationDirPath()+"/bj1.jpg")));//背景图片
    setPalette(pal);

//    timer = new QTimer(this);
//    connect(timer,SLOT(timeout),this, SLOT(cd()));
//    timer->start(100);




    playProcess =new QProcess(this);
//    connect(playProcess,SIGNAL(readyReadStandardOutput()),this,SLOT(redate()));
//    QTimer *Timer;
    Timer = new QTimer(this);
    connect(Timer, SIGNAL(timeout()), this, SLOT(handleTimeout()));
    Timer->start(100);
//    QFile file(":/youjing.qss");
//    file.open(QFile::ReadOnly);
//    this->setStyleSheet(file.readAll());
    ui->list_lrc->setVerticalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
    ui->list_lrc->setHorizontalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
    ui->Pause->setStyleSheet(tr("border-image: url(:/img/play.png);"));
    ui->label->setStyleSheet(tr("border-image: url(:/img/music.jpg);"));
    ui->label->hide();





}
 static  QString notice="Which media will play";
 static  QString path="/media";
 static  QString type="video(*.mp4 *.avi *.mp3 *.m4a)";

Media::~Media()
{
    playProcess->kill();
    delete ui;
}

void Media::on_Add_clicked()
{
    this->update();
    //playProcess->kill();
    //QStringList mediafile = QFileDialog::getOpenFileNames(this,"Which media will play","/home","video(*.mp4 *.wmv *.avi *.mp3 *.m4a)");
    QStringList mediafile = QFileDialog::getOpenFileNames(this,notice,path,type);
    if(mediafile.length()<=0){
        return;
    }

    QRegExp re("[^/.\w+.]+");


    for (int i=0;i<mediafile.count();i++)
    {
        QFileInfo fileInfo = QFileInfo(mediafile.at(i));
        QString media_name = fileInfo.fileName();
//        qDebug()<<"54848"<<media_name;

        qDebug()<<"media_name"<<media_name;
        if(media_name.indexOf(re) >= 0)
          {

//            qDebug()<<"re.cap(0)"<<re.cap(0);
            map_song_path.insert(re.cap(0),mediafile.at(i));
            new QListWidgetItem(re.cap(0),ui->listWidget);

          }
    }

    if(mediafile.count()!=0){

//        ui->listWidget->addItems(mediafile);

    }

//    play(mediafile.at(0));
    this->update();
}



static int m=0;
static int s=0;
static int d=0;
void Media::play(QString filename){
    ui->Pause->setStyleSheet(tr("border-image: url(:/img/pause.png);"));
    pause=false;
    Timer->stop();
    int lrc_row=0;
    m=0;
    s=0;
    d=0;

    this->playProcess->kill();

    if(!this->playProcess->waitForFinished(3000)){qDebug()<<"waitForFinished";}
    ui->horizontalSlider->clearFocus();
    ui->list_lrc->clear();
    ui->lrc_now->clear();
    ui->lrc_next->clear();
    ui->medianow->clear();




    ui->lrc_now->clear();
    int counter =ui->list_lrc->count();
    for(int index = 0;index < counter; index++)
    {
        QListWidgetItem *item = ui->list_lrc->takeItem(0);
            delete item;
    }

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

        for (int i=0;i<lrc_mid;i++) {
            new QListWidgetItem(" ",ui->list_lrc);
        }
        while((line=file.readLine())>0){
            QRegExp re("[0-9]{2}:[0-9]{2}.[0-9]{1}");
            if(line.indexOf(re) >= 0)
              {
                QString lrc_time= re.cap(0);
                QString lrc_text=line.section(QRegExp("[]\s^\n]"),1,1);

                map_lrc.insert(lrc_time, lrc_text);
                map_lrc_time.insert(lrc_time,QString::number(lrc_row));
                lrc_row++;
//                ui->list_lrc->addItem(lrc_text);
                QListWidgetItem *item_now = new QListWidgetItem(lrc_text,ui->list_lrc);
                item_now ->setTextAlignment(Qt::AlignCenter);

              }
        }
        for (int i=0;i<lrc_mid;i++) {
            new QListWidgetItem(" ",ui->list_lrc);
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
//    this->playProcess->waitForStarted(3000);
    connect(playProcess,SIGNAL(readyReadStandardOutput()),this,SLOT(redate()));
    this->playProcess->write("get_time_length\n");
//    Timer->stop();
    Timer->start(100);


}

void Media::on_listWidget_itemDoubleClicked(QListWidgetItem *item)
{
//    this->playProcess->kill();
    play(map_song_path.value(item->text()));
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
//    Timer->stop();
    this->playProcess->write("pause\n");
//    qDebug()<<"pause"<<pause;
    if(pause){
        ui->Pause->setStyleSheet(tr("border-image: url(:/img/play.png);"));
        Timer->stop();
        disconnect(playProcess,SIGNAL(readyReadStandardOutput()),this,SLOT(redate()));

    }
    if(!pause){
        ui->Pause->setStyleSheet(tr("border-image: url(:/img/pause.png);"));


        Timer->start(100);
        connect(playProcess,SIGNAL(readyReadStandardOutput()),this,SLOT(redate()));


//        playProcess->write("get_time_length\n");//发命令，cmd命令
//        playProcess->write("get_time_pos\n");
//        playProcess->write("get_percent_pos\n");
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

        play(map_song_path.value(item));
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
    play(map_song_path.value(item));
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

static QString pre_lrc="";
static int set_row=7;
static QString mins;static QString min2s;
static QString secs;static QString sec2s;
static QString ds;
void Media::redate(){

//    this->playProcess->write("get_time_length\n");
//    this->playProcess->write("get_time_pos\n");
//    this->playProcess->write("get_percent_pos\n");

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
           mins=QString::number(int(Str.toDouble())/60,10); secs=QString::number(int(Str.toDouble())%60,10);
           ds=QString::number((Str.toDouble()-qFloor(int(Str.toDouble())))*10);

           ui->timetext->setText(mins+":"+secs);

           QString timetips="";
           if (secs.toInt()<10){
               timetips="0"+mins+":"+"0"+secs+"."+ds;
           }else {
               timetips="0"+mins+":"+secs+"."+ds;
           }
//           qDebug()<<"mins"<<mins<<"secs"<<secs<<"ds"<<ds<<"timetips"<<timetips;
           QString time_lrc=map_lrc.value(timetips);
//           ui->lrc_now->setText(time_lrc);

           QMap<QString, QString>::iterator iter = map_lrc.begin();
           while (iter != map_lrc.end())
           {
               if(iter.key()==timetips)
               {

                   QList<QListWidgetItem *> str_lrc_list;
                   if(pre_lrc==time_lrc){return;}
                   str_lrc_list=ui->list_lrc->findItems(time_lrc,Qt::MatchExactly);
                   pre_lrc=time_lrc;
//                   int set_row=ui->list_lrc->row(str_lrc_list.at(0));

                   set_row++;
                   int row_now=map_lrc_time.value(timetips).toInt()+lrc_mid;

                    if(row_now<ui->list_lrc->count()-lrc_mid)
                    {
                        ui->list_lrc->setCurrentRow(row_now);
                        if (row_now>lrc_mid){ui->list_lrc->verticalScrollBar()->setValue(row_now-lrc_mid);}
                    }

                   ui->lrc_now->setText(time_lrc);
//                   qDebug()<<"row_now"<<row_now-8<<"map_lrc.count()-1"<<map_lrc.value((iter+1).key());

                    if(row_now-lrc_mid<(map_lrc.count()-1)){ui->lrc_next->setText(map_lrc.value((iter+1).key()));}
                    else{ui->lrc_next->clear();}
//
               }
                   iter++;
           }

        }
        else if((order.startsWith("ANS_LENGTH")))
        {
           order.replace(QByteArray("\n"),QByteArray(""));
           QString content(order);
           Time=content.mid(11).simplified();

           //qDebug()<<mins<<":"<<secs<<endl;

            min2s=QString::number(int(Time.toDouble())/60,10); sec2s=QString::number(int(Time.toDouble())%60,10);
           //qDebug()<<min2s<<":"<<sec2s<<endl;

//           qDebug()<<timetips<<":"<<time_lrc<<endl;


           ui->timetext_2->setText(min2s+":"+sec2s);


           ui->horizontalSlider->setRange(0,qFloor(Time.toDouble()));
        }
        if((mins.toInt()*60+secs.toInt())==(min2s.toInt()*60+sec2s.toInt()-2))
        {on_next_song_clicked();
        mins="0";secs="0";min2s="0";sec2s="0";

        }
//        qDebug()<<mins.toInt()*60+secs.toInt()<<"min2s.toInt"<<min2s.toInt()*60+sec2s.toInt()-2<<endl;

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
        ui->label_2->hide();
        ui->label->show();

        }
    else {
        ui->listWidget->show();
        ui->label_2->show();
        ui->label->hide();
}

}

void Media::handleTimeout()
{
    cd();
//    qDebug()<<"Enter timeout processing function\n";
//    QString sec="";
//    QString min="";
//    d++;
//    if(d>=10){d=0;s++;}
//    if(s>=60){s=0;m++;}
//    if(m>=60){m=0;}
//    if(s<10){sec="0"+QString::number(s);}else{sec=QString::number(s);}
//    if(m<10){min="0"+QString::number(m);}else{min=QString::number(m);}

    this->playProcess->write("get_time_pos\n");

//    QString timetips=min+":"+sec+"."+QString::number(d);
////    qDebug()<<"timetips"<<timetips;
//    QString time_lrc=map_lrc.value(timetips);
////           ui->lrc_now->setText(time_lrc);

//    QMap<QString, QString>::iterator iter = map_lrc.begin();
//    while (iter != map_lrc.end())
//    {
//        if(iter.key()==timetips)
//        {

//            QList<QListWidgetItem *> str_lrc_list;
//            if(pre_lrc==time_lrc){return;}
//            str_lrc_list=ui->list_lrc->findItems(time_lrc,Qt::MatchExactly);
//            pre_lrc=time_lrc;
////                   int set_row=ui->list_lrc->row(str_lrc_list.at(0));

//            set_row++;
//            int row_now=map_lrc_time.value(timetips).toInt()+8;

//             if(row_now<ui->list_lrc->count()-8)
//             {
//                 ui->list_lrc->setCurrentRow(row_now);
//                 if (row_now>8){ui->list_lrc->verticalScrollBar()->setValue(row_now-8);}
//             }

//            ui->lrc_now->setText(time_lrc);

//            ui->lrc_next->setText(map_lrc.value((iter+1).key()));
//        }
//            iter++;
//    }



}
static bool paint=false;
void Media::paintEvent(QPaintEvent *event)
{
    if(!paint){return;}

    Q_UNUSED(event)
    QPainter painter(this);

    painter.setPen(Qt::NoPen);

    painter.setRenderHint(QPainter::Antialiasing, true);
    QPoint centerPoint = rect().center();
    centerPoint.setY(245);

    painter.save();
    painter.translate(centerPoint);
    int radius = 60;
    QPen pen;
    pen.setColor(Qt::gray);
    pen.setWidth(1);
    painter.setPen(pen);
    painter.drawEllipse(QPoint(0, 0), radius, radius);

    QRect rect = QRect(-radius, -radius,
                       radius*2, radius*2);

    pen.setColor(Qt::blue);
    painter.setPen(pen);
    QRegion maskedRegion(rect, QRegion::Ellipse);
    painter.setClipRegion(maskedRegion);
    painter.rotate(angle);

    painter.drawPixmap(rect,QPixmap(":/img/cd2.png"));
    painter.restore();
}
void Media::cd()
{
    angle += 2;
    if(angle == 360.0)angle = 0.0;
    update();
}
