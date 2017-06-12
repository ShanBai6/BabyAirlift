package com.mycompany.app;

import java.util.Date;
import java.text.SimpleDateFormat;

public class CurrentTime
{
    public String getTime(){
        Date time = new Date();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
    }
}
