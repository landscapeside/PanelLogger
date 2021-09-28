// ILoggerInterface.aidl
package com.landside.panellogger;

// Declare any non-default types here with import statements
import com.landside.panellogger.LogItem;


interface ILoggerInterface {

    void recordLog(in LogItem item);
}