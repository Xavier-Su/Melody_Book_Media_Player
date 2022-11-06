#include <QApplication>
#include <QWidget>
#include "media.h"
#include <QPushButton>

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);

//    QTextCodec::setCodecForTr(QTextCodec::codecForName("UTF-8"));
//    QTextCodec::setCodecForLocale(QTextCodec::codecForName("UTF-8"));
    QTextCodec::setCodecForCStrings(QTextCodec::codecForName("UTF-8"));
    Media *m=new Media;
    m->show();

    return a.exec();
}
