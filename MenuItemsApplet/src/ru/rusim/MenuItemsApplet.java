package ru.rusim;

import javacard.framework.AID;
import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.Shareable;
import uicc.toolkit.EnvelopeHandler;
import uicc.toolkit.EnvelopeHandlerSystem;
import uicc.toolkit.ProactiveHandler;
import uicc.toolkit.ProactiveHandlerSystem;
import uicc.toolkit.ToolkitConstants;
import uicc.toolkit.ToolkitInterface;
import uicc.toolkit.ToolkitRegistry;
import uicc.toolkit.ToolkitRegistrySystem;

public class MenuItemsApplet extends Applet implements ToolkitInterface, ToolkitConstants
{
    public ToolkitRegistry tr;
    private byte[] menuEntry_1 = {(byte)'M', (byte)'e', (byte)'n', (byte)'u', (byte)' ', (byte)'1'};
    private byte[] menuEntry_2 = {(byte)'M', (byte)'e', (byte)'n', (byte)'u', (byte)' ', (byte)'2'};
    private byte menuId_1, menuId_2;

    final byte[] timerValue = {(byte)0x00, (byte)0x01, (byte)0x00};
    byte timerId;

    public MenuItemsApplet()
    {

    }

    public static void install(byte bArray[], short bOffset, byte bLength)
    {
        MenuItemsApplet app = new MenuItemsApplet();
        app.register();
        app.tr = ToolkitRegistrySystem.getEntry();
        app.menuId_1 = app.tr.initMenuEntry(app.menuEntry_1, (short)0, (short)app.menuEntry_1.length, (byte)0, false, (byte)0, (short)0);
        app.menuId_2 = app.tr.initMenuEntry(app.menuEntry_2, (short)0, (short)app.menuEntry_2.length, (byte)0, false, (byte)0, (short)0);
        app.timerId = app.tr.allocateTimer();
    }

    public void process(APDU apdu){}

    public void processToolkit(short event)
    {
        switch (event) {
            case EVENT_MENU_SELECTION:
                EnvelopeHandler envHdlr = EnvelopeHandlerSystem.getTheHandler();
                byte menuId = envHdlr.getItemIdentifier();
                if (menuId == menuId_1)
                {

                }
                else if (menuId == menuId_2)
                {

                } 
            break;
            
            case EVENT_TIMER_EXPIRATION:
                ProactiveHandler proHdlr = ProactiveHandlerSystem.getTheHandler();
                proHdlr.init(PRO_CMD_TIMER_MANAGEMENT, (byte)0x00, DEV_ID_TERMINAL);
                proHdlr.appendTLV(TAG_TIMER_IDENTIFIER, timerId);
                proHdlr.appendTLV(TAG_TIMER_VALUE, timerValue, (short)0x00, (short)timerValue.length);
                proHdlr.send();
            break;
            
            default: break;
        }
    }

    public Shareable getshareableInterfaceObject(AID aid, byte param)
    {
        if ((null == aid) && (byte)1 == param)
            return this;
        return null;
    }
}
