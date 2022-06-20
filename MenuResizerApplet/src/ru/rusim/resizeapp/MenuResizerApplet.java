package ru.rusim.resizeapp;

import uicc.system.*;
import uicc.access.*;
import uicc.access.fileadministration.*;
import uicc.toolkit.*;
import javacard.framework.*;

public class MenuResizerApplet extends Applet implements ToolkitInterface
{
    AdminFileView fvDF_TELECOM; // An instance of AdminFileView class intended to select DF Telecom which is used to perform resizing of EF 6F54
    FileView fvEF_SUME;         // An instance of FileView class intended to select and update the EF Sume which resides under DF Telecom
    static final short FID_EF_SUME = (short)0x6F54;

    // Title of the menu item allocated to the application
    private static byte[] menuTitle ={(byte)'C', (byte)'h', (byte)'a', (byte)'n', (byte)'g', (byte)'e',
                                      (byte)' ', (byte)'t', (byte)'i', (byte)'t', (byte)'l', (byte)'e'};
    // Hint shown to the user to get a new menu title
    private static byte[] userHint = {(byte)'I', (byte)'n', (byte)'s', (byte)'e', (byte)'r', (byte)'t', (byte)' ',
                                      (byte)'t', (byte)'h', (byte)'e',  (byte)' ', (byte)'n', (byte)'e', (byte)'w', (byte)' ',
                                      (byte)'t', (byte)'i', (byte)'t', (byte)'l', (byte)'e', (byte)':'};
    // Template for the resize command
    private static byte[] resizeCommand = {(byte)0x83, (byte)0x02, (byte)0x6F, (byte)0x54,
                                           (byte)0x80,(byte)0x02, (byte)0x00, (byte)0x00};
    
    private final short CMD_RESIZE_LENGTH_OFFSET = (short)6;
    private static ToolkitRegistry tkRegistry;
    private EditHandler editHandler;    // Reference to an Edit Handler object, used to execute the RESIZE command

    public void initToolkit()
    {
        tkRegistry = ToolkitRegistrySystem.getEntry();  // Registering to the Toolkit
        tkRegistry.initMenuEntry(menuTitle, (short)0, (short)menuTitle.length,
                                (byte)0, false, (byte)0, (short)0);
        
        fvDF_TELECOM = AdminFileViewBuilder.getTheUICCAdminFileView(JCSystem.NOT_A_TRANSIENT_OBJECT);
        fvEF_SUME = UICCSystem.getTheUICCView(JCSystem.NOT_A_TRANSIENT_OBJECT);

        fvEF_SUME.select(UICCConstants.FID_DF_TELECOM);
        fvEF_SUME.select(FID_EF_SUME);

        fvDF_TELECOM.select(UICCConstants.FID_DF_TELECOM);
        // The Edit Handler is allocated with exactly size of the RESIZE command template
        editHandler = (EditHandler)HandlerBuilder.buildTLVHandler(HandlerBuilder.EDIT_HANDLER,
                                                                  (short)resizeCommand.length);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength)
    {
        MenuResizerApplet app = new MenuResizerApplet();
        app.register();
        app.initToolkit();
    }

    @Override
    public void processToolkit(short event) throws ToolkitException
    {
        byte res;
        byte[] globalBuffer;
        short dataLen;
        
        if (ToolkitConstants.EVENT_MENU_SELECTION == event)
        {
            ProactiveHandler proHdlr = ProactiveHandlerSystem.getTheHandler();
            // Proactive Handler sends the hint to the user via GET_INPUT command 
            proHdlr.initGetInput((byte)0x00, (byte)0x04, userHint, (short)0, (short)userHint.length, (short)1, (short)20);
            res = proHdlr.send();

            if ((byte)0x00 == res)  // have pressed pressed 'OK'?
            {
                ProactiveResponseHandler proRespHdlr = ProactiveResponseHandlerSystem.getTheHandler();
                globalBuffer = UICCPlatform.getTheVolatileByteArray();
                dataLen = proRespHdlr.copyTextString(globalBuffer, (short)2);   // starting from globalBuffer's idx 2, proceed to copying Text string 
                // indices being skipped at previous instruction
                globalBuffer[0] = ToolkitConstants.TAG_ALPHA_IDENTIFIER;
                globalBuffer[1] = (byte)(dataLen - 2);

                resizeMenuFile(dataLen);
                fvEF_SUME.updateBinary((short)0, globalBuffer, (short)0, dataLen);
            }
        }
    }

    public void resizeMenuFile(short newFileLength)
    {
        Util.setShort(resizeCommand, CMD_RESIZE_LENGTH_OFFSET, newFileLength);
        editHandler.clear();
        editHandler.appendArray(resizeCommand, (short)0, (short)resizeCommand.length);
        fvDF_TELECOM.resizeFile(editHandler);
    }

    @Override
    public void process(APDU arg0) throws ISOException {}

    public Shareable getShareableInterfaceObject(AID clientAID, byte parameter)
    {
        if (null == clientAID)
            return (Shareable)this;
        
        return null;
    }
}