package ru.uicc.test;

import uicc.toolkit.*;
import uicc.access.*;
import javacard.framework.*;

/**
 * ETSI TS 102 224 Toolkit Applet Example
 */
public class AppletExample extends Applet implements ToolkitInterface, ToolkitConstants
{
    public ToolkitRegistry tr;
    private byte[] menuEntry = {(byte) '1', (byte) '0', (byte) '2', (byte) ' ',
                                (byte) '2', (byte) '4', (byte) '1', (byte) ' ',
                                (byte) 'A', (byte) 'p', (byte) 'p', (byte) 'l', (byte) 'e', (byte) 't'
                            };
    private byte itemId;

    public AppletExample()
    {
        // stub
    }

    public static void install(byte bArray[], short bOffset, byte bLength)
    {
        AppletExample thisApplet = new AppletExample();
        thisApplet.register();
        thisApplet.tr = ToolkitRegistrySystem.getEntry();
        thisApplet.itemId = thisApplet.tr.initMenuEntry(thisApplet.menuEntry, (short)0, (short)thisApplet.menuEntry.length, (byte)0, false, (byte)0, (short)0);
    }

    @Override
    public void processToolkit(short event) throws ToolkitException
    {
        switch (event)
        {
            case (byte) 1: break;
            case (byte) 2: break;
            case (byte) 3: break;    
            default: break;
        }
    }

    @Override
    public void process(APDU apdu) throws ISOException
    {
        // do nothing
    }
    
    public Shareable getShareableInterfaceObject(AID aid, byte param)
    {
        if ((null == aid) && ((byte)1 == param))
            return this;
        
        return null;
    }
}
