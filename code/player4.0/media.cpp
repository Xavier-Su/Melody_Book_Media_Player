
#include "media.h"
#include "ui_media.h"
#include<QPushButton>
#include <QMessageBox>
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
static bool paint=false;
static bool cd_show=false;
static bool cd_pause=true;
static bool play_state=false;
Media::Media(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::Media)
{

    ui->setupUi(this);
    setWindowTitle("音书v1.0  -by素白");
    this->setWindowIcon(QIcon(":/img/logo.png"));
    QPalette pal =this->palette();
    pal.setBrush(QPalette::Background,QBrush(QPixmap(QCoreApplication::applicationDirPath()+"/bj1.jpg")));//背景图片
//    pal.setBrush(QPalette::Background,QBrush(QPixmap(QCoreApplication::applicationDirPath()+"/bg2.png")));//背景图片

    setPalette(pal);

    //    timer = new QTimer(this);
    //    connect(timer,SLOT(timeout),this, SLOT(cd()));
    //    timer->start(100);

    playProcess =new QProcess(this);

    Timer = new QTimer(this);
    connect(Timer, SIGNAL(timeout()), this, SLOT(handleTimeout()));
    Timer->start(100);
    //    QFile file(":/youjing.qss");
    //    file.open(QFile::ReadOnly);
    //    this->setStyleSheet(file.readAll());
    ui->list_lrc->setVerticalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
    ui->list_lrc->setHorizontalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
    ui->Pause->setStyleSheet(tr("border-image: url(:/img/play.png);"));
    ui->label->setStyleSheet(tr("border-image: url(:/img/logo.png);"));
    ui->label->hide();
    ui->horizontalSlider->setTracking(false);


}
static  QString notice="Which media will be played";
static  QString path="/media";
static  QString type="video(*.mp4 *.avi *.mp3 *.m4a)";
static  QString notice_skin="Please select a 1024x600 image";
static  QString type_skin="images(*.jpg *.png)";


Media::~Media()
{
    playProcess->kill();
    delete ui;
}

void Media::on_Add_clicked()
{
    this->update();
    //playProcess->kill();
    QStringList mediafile = QFileDialog::getOpenFileNames(this,notice,path,type);
    if(mediafile.length()<=0){
        return;
    }

    QRegExp re("[^/.\w+.]+");

    for (int i=0;i<mediafile.count();i++)
    {
        QFileInfo fileInfo = QFileInfo(mediafile.at(i));
        QString media_name = fileInfo.fileName();
        qDebug()<<"media_name"<<media_name;
        if(media_name.indexOf(re) >= 0)
        {
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
                QListWidgetItem *item_now = new QListWidgetItem(lrc_text,ui->list_lrc);
                item_now ->setTextAlignment(Qt::AlignCenter);

            }
        }
        for (int i=0;i<lrc_mid;i++) {
            new QListWidgetItem(" ",ui->list_lrc);
        }
    }

    QString program =QCoreApplication::applicationDirPath()+"/mplayer/mplayer.exe";
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


    QRegExp re("[^/.\w+.]+");
    if(media_name.indexOf(re) >= 0)
    {
        media_name= re.cap(0);
    }
    ui->medianow->setText("正在播放: "+media_name);

    this->playProcess->start(program,commond);
    play_state=true;
    cd_pause=false;

    connect(playProcess,SIGNAL(readyReadStandardOutput()),this,SLOT(redate()));
    this->playProcess->write("get_time_length\n");
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
    if((ui->listWidget->count()==0) && (play_state==false)){
        msg_inf();
        return;

    }

    pause=!pause;
    this->playProcess->write("pause\n");
    if(pause){
        cd_pause=true;
        ui->Pause->setStyleSheet(tr("border-image: url(:/img/play.png);"));
        Timer->stop();
        disconnect(playProcess,SIGNAL(readyReadStandardOutput()),this,SLOT(redate()));
    }
    if(!pause){
        cd_pause=false;
        ui->Pause->setStyleSheet(tr("border-image: url(:/img/pause.png);"));
        Timer->start(100);
        connect(playProcess,SIGNAL(readyReadStandardOutput()),this,SLOT(redate()));
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
    QString item = ui->listWidget->currentItem()->text();
    play(map_song_path.value(item));

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

        if(order.startsWith("ANS_TIME_POSITION"))
        {
            order.replace(QByteArray("\n"),QByteArray(""));
            QString content(order);
            Str=content.mid(18).simplified();
            int time_position=qFloor(Str.toDouble());
            ui->horizontalSlider->setValue(time_position);

            mins=QString::number(int(Str.toDouble())/60,10);
            secs=QString::number(int(Str.toDouble())%60,10);
            ds=QString::number((Str.toDouble()-qFloor(int(Str.toDouble())))*10);
            ui->timetext->setText(mins+":"+secs);

            QString timetips="";
            if (secs.toInt()<10){
                timetips="0"+mins+":"+"0"+secs+"."+ds;
            }else {
                timetips="0"+mins+":"+secs+"."+ds;
            }

            QString time_lrc=map_lrc.value(timetips);
            QMap<QString, QString>::iterator iter = map_lrc.begin();
            while (iter != map_lrc.end())
            {
                if(iter.key()==timetips)
                {
                    QList<QListWidgetItem *> str_lrc_list;
                    if(pre_lrc==time_lrc){return;}
                    str_lrc_list=ui->list_lrc->findItems(time_lrc,Qt::MatchExactly);
                    pre_lrc=time_lrc;

                    int row_now=map_lrc_time.value(timetips).toInt()+lrc_mid;
                    if(row_now<ui->list_lrc->count()-lrc_mid)
                    {
                        ui->list_lrc->setCurrentRow(row_now);
                        if (row_now>lrc_mid){ui->list_lrc->verticalScrollBar()->setValue(row_now-lrc_mid);}
                    }

                    ui->lrc_now->setText(time_lrc);
                    if(row_now-lrc_mid<(map_lrc.count()-1)){ui->lrc_next->setText(map_lrc.value((iter+1).key()));}
                    else{ui->lrc_next->clear();}

                }
                iter++;
            }

        }
        else if((order.startsWith("ANS_LENGTH")))
        {
            order.replace(QByteArray("\n"),QByteArray(""));
            QString content(order);
            Time=content.mid(11).simplified();
            min2s=QString::number(int(Time.toDouble())/60,10); sec2s=QString::number(int(Time.toDouble())%60,10);
            ui->timetext_2->setText(min2s+":"+sec2s);
            ui->horizontalSlider->setRange(0,qFloor(Time.toDouble()));
        }
        if((mins.toInt()*60+secs.toInt())==(min2s.toInt()*60+sec2s.toInt()-1))
        {   mins="0";secs="0";min2s="0";sec2s="0";
            on_next_song_clicked();

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
    case Qt::Key_O://歌词关
//        ui->horizontalSlider->hide();
//        ui->timetext->hide();
//        ui->timetext_2->hide();
        ui->lrc_now->hide();
        ui->lrc_next->hide();
        break;
    case Qt::Key_U://歌词开
//        ui->horizontalSlider->show();
//        ui->timetext->show();
//        ui->timetext_2->show();
        ui->lrc_now->show();
        ui->lrc_next->show();
        break;

    }
}




static int tips=0;
void Media::on_horizontalSlider_sliderMoved(int position)
{
    tips=position;
}

void Media::on_horizontalSlider_sliderReleased()
{
    this->playProcess->write(QString("seek "+QString::number(tips)+" 2\n").toUtf8());
    on_Pause_clicked();
}



void Media::on_horizontalSlider_sliderPressed()
{           on_Pause_clicked();
            //qDebug()<<"Pressed"<<QString::number(ui->horizontalSlider->value())<<endl;
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
    //    QString sec="";
    //    QString min="";
    //    d++;
    //    if(d>=10){d=0;s++;}
    //    if(s>=60){s=0;m++;}
    //    if(m>=60){m=0;}
    //    if(s<10){sec="0"+QString::number(s);}else{sec=QString::number(s);}
    //    if(m<10){min="0"+QString::number(m);}else{min=QString::number(m);}
    this->playProcess->write("get_time_pos\n");

}

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
    int radius = 150;
    QPen pen;
    pen.setColor(Qt::transparent);
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

//    painter.drawPixmap(rect,QPixmap(":/cd_music-cd.png"));
    painter.drawPixmap(rect,QPixmap(":/img/cd.png"));
    painter.restore();
}
void Media::cd()
{
    if(cd_show){
    if(cd_pause){angle += 0;cd();}else {angle += 1;}
    }if(angle == 360.0)angle = 0.0;
    update();
}

void Media::on_change_skin_clicked()
{
    QMessageBox msgBox;
     //1.标题
    msgBox.setWindowTitle("温馨提示：");
    // 2.内容
     msgBox.setText("恭喜你发现切换皮肤的功能");
     msgBox.setInformativeText("请优先选择1024x600尺寸的图片 以实现最好效果");
     msgBox.setStandardButtons(QMessageBox::Ok);
//         //4.图标
//     QPixmap pixmap(":/img/add-music.png");
//     msgBox.setIconPixmap(pixmap);
     //5.给按键取别名
     msgBox.setButtonText(QMessageBox::Ok,"明白");
     //6.设置默认按键
     msgBox.setDefaultButton(QMessageBox::Ok);
     msgBox.exec();

    QStringList skin = QFileDialog::getOpenFileNames(this,notice_skin,path,type_skin);
    if(skin.length()<=0){
        return;
    }

    QPalette pal =this->palette();
    pal.setBrush(QPalette::Background,QBrush(QPixmap(skin.at(0))));//背景图片

//    pal.setBrush(QPalette::Background,QBrush(QPixmap(QCoreApplication::applicationDirPath()+"/bg2.png")));//背景图片

    setPalette(pal);
}

void Media::on_cd_circle_clicked()
{
    if(play_state)
    {
        cd_show = !cd_show;
        if(cd_show){paint=true;}
        if(!cd_show){paint=false;}
    }else {
    msg_inf();return;
}


}

void Media::msg_inf()
{
    QMessageBox msgBox;
     //1.标题
    msgBox.setWindowTitle("温馨提示：");
    // 2.内容
     msgBox.setText("未有文件播放中 请在左侧寻找该图标添加文件");
     msgBox.setInformativeText("添加文件后 选中双击鼠标 开始播放文件后该按钮则正常使用。");
     msgBox.setStandardButtons(QMessageBox::Ok);
//         //4.图标
     QPixmap pixmap(":/img/add-music.png");
     msgBox.setIconPixmap(pixmap);
     //5.给按键取别名
     msgBox.setButtonText(QMessageBox::Ok,"好的");
     //6.设置默认按键
     msgBox.setDefaultButton(QMessageBox::Ok);
     msgBox.exec();
}
