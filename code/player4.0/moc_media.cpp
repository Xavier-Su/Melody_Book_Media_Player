/****************************************************************************
** Meta object code from reading C++ file 'media.h'
**
** Created by: The Qt Meta Object Compiler version 63 (Qt 4.8.7)
**
** WARNING! All changes made in this file will be lost!
*****************************************************************************/

#include "media.h"
#if !defined(Q_MOC_OUTPUT_REVISION)
#error "The header file 'media.h' doesn't include <QObject>."
#elif Q_MOC_OUTPUT_REVISION != 63
#error "This file was generated using the moc from 4.8.7. It"
#error "cannot be used with the include files from this version of Qt."
#error "(The moc has changed too much.)"
#endif

QT_BEGIN_MOC_NAMESPACE
static const uint qt_meta_data_Media[] = {

 // content:
       6,       // revision
       0,       // classname
       0,    0, // classinfo
      11,   14, // methods
       0,    0, // properties
       0,    0, // enums/sets
       0,    0, // constructors
       0,       // flags
       0,       // signalCount

 // slots: signature, parameters, type, tag, flags
      13,    7,    6,    6, 0x08,
      39,    6,    6,    6, 0x08,
      58,    6,    6,    6, 0x08,
      75,    6,    6,    6, 0x08,
      96,    6,    6,    6, 0x08,
     118,    6,    6,    6, 0x08,
     136,    6,    6,    6, 0x08,
     158,    6,    6,    6, 0x08,
     187,  182,    6,    6, 0x08,
     237,    6,    6,    6, 0x08,
     259,    6,    6,    6, 0x08,

       0        // eod
};

static const char qt_meta_stringdata_Media[] = {
    "Media\0\0event\0keyPressEvent(QKeyEvent*)\0"
    "on_Pause_clicked()\0on_Add_clicked()\0"
    "on_Forward_clicked()\0on_Backward_clicked()\0"
    "on_Exit_clicked()\0on_volumeup_clicked()\0"
    "on_volumedown_clicked()\0item\0"
    "on_listWidget_itemDoubleClicked(QListWidgetItem*)\0"
    "on_delete_2_clicked()\0redate()\0"
};

void Media::qt_static_metacall(QObject *_o, QMetaObject::Call _c, int _id, void **_a)
{
    if (_c == QMetaObject::InvokeMetaMethod) {
        Q_ASSERT(staticMetaObject.cast(_o));
        Media *_t = static_cast<Media *>(_o);
        switch (_id) {
        case 0: _t->keyPressEvent((*reinterpret_cast< QKeyEvent*(*)>(_a[1]))); break;
        case 1: _t->on_Pause_clicked(); break;
        case 2: _t->on_Add_clicked(); break;
        case 3: _t->on_Forward_clicked(); break;
        case 4: _t->on_Backward_clicked(); break;
        case 5: _t->on_Exit_clicked(); break;
        case 6: _t->on_volumeup_clicked(); break;
        case 7: _t->on_volumedown_clicked(); break;
        case 8: _t->on_listWidget_itemDoubleClicked((*reinterpret_cast< QListWidgetItem*(*)>(_a[1]))); break;
        case 9: _t->on_delete_2_clicked(); break;
        case 10: _t->redate(); break;
        default: ;
        }
    }
}

const QMetaObjectExtraData Media::staticMetaObjectExtraData = {
    0,  qt_static_metacall 
};

const QMetaObject Media::staticMetaObject = {
    { &QMainWindow::staticMetaObject, qt_meta_stringdata_Media,
      qt_meta_data_Media, &staticMetaObjectExtraData }
};

#ifdef Q_NO_DATA_RELOCATION
const QMetaObject &Media::getStaticMetaObject() { return staticMetaObject; }
#endif //Q_NO_DATA_RELOCATION

const QMetaObject *Media::metaObject() const
{
    return QObject::d_ptr->metaObject ? QObject::d_ptr->metaObject : &staticMetaObject;
}

void *Media::qt_metacast(const char *_clname)
{
    if (!_clname) return 0;
    if (!strcmp(_clname, qt_meta_stringdata_Media))
        return static_cast<void*>(const_cast< Media*>(this));
    return QMainWindow::qt_metacast(_clname);
}

int Media::qt_metacall(QMetaObject::Call _c, int _id, void **_a)
{
    _id = QMainWindow::qt_metacall(_c, _id, _a);
    if (_id < 0)
        return _id;
    if (_c == QMetaObject::InvokeMetaMethod) {
        if (_id < 11)
            qt_static_metacall(this, _c, _id, _a);
        _id -= 11;
    }
    return _id;
}
QT_END_MOC_NAMESPACE
