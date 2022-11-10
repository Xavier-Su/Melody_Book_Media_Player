#ifndef MEDIA_H
#define MEDIA_H

#include <QWidget>
#include <QFileDialog>
#include <QProcess>
#include <QString>
#include <QTimer>
#include <QKeyEvent>
#include <QWidget>
#include <QProcess>
#include <QtGui>
#include "QSpinBox"
#include "QListWidgetItem"
#include "QMessageBox"
#include "QFileDialog"
#include "QInputDialog"
#include "QLabel"

namespace Ui {
class Media;
}
class Media : public QMainWindow
{
    Q_OBJECT

public:
    explicit Media(QWidget *parent = 0);
    ~Media();



private slots:
    void keyPressEvent(QKeyEvent *event);

    void on_Pause_clicked();

    void on_Add_clicked();

    void on_Forward_clicked();

    void on_Backward_clicked();

    void on_Exit_clicked();

    void on_volumeup_clicked();

    void on_volumedown_clicked();

    void on_listWidget_itemDoubleClicked(QListWidgetItem *item);

    void on_delete_2_clicked();

    void redate();

    void on_horizontalSlider_sliderMoved(int position);

    void on_horizontalSlider_sliderReleased();

    void on_horizontalSlider_sliderPressed();

    void on_next_song_clicked();

    void on_previous_song_clicked();

    void on_list_button_clicked();
    void handleTimeout();
    void paintEvent(QPaintEvent *event);
    void cd();

    void on_change_skin_clicked();

    void on_cd_circle_clicked();
    void msg_inf();

private:
    Ui::Media *ui;
    QStringList playList;
    int playIndex;
    QProcess *playProcess;
    QString Str;
    QString Time;
    QTimer *myTimer;
    bool pause;
    QProcess *p;
    void play(const QString filename);
    double angle;


};

#endif // Media_H
