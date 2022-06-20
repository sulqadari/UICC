package ru.rusim;
import uicc.system.*;
import uicc.access.*;
import uicc.access.fileadministration.*;
import uicc.toolkit.*;
import uicc.usim.toolkit.ToolkitConstants;
import javacard.framework.*;

public class PhoneBookMonitorApplet extends Applet implements ToolkitInterface
{
    // The first object reference is used to select DF telecom under MF, and the second - under ADF USIM
    // Both intended to perform file view system monitoring
    FileView fvDF_PB_TELECOM, fvDF_PB_ADF;
    static final short FID_DF_PHONEBOOK = (short)0x5F3A;
    static final byte PRO_CMD_SMS = (byte)0x13;
    static final byte PRO_CMD_SMS_TPDU_TAG = (byte)0x0B;    // Tag in the Send SM Proactive command indicating a TPDU
    // AID of na USIM application
    private static byte[] usim_AID = {
        (byte)0xA0, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x87, (byte)0x10, (byte)0x02, (byte)0xFF,
        (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x89,
        (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00
    };

    //Destination address of the SMS. It must be updated with a valid value
    private static byte[] destAddress = {(byte)0x06, (byte)0x33, (byte)0x24, (byte)0x35, (byte)0x21};
    
    private static ToolkitRegistry tkRegistry;
    // Used to store the file list when the applet is triggered in reentrancy
    private EditHandler editHandler;

    private static final byte TAG_UPDATE_INFO = (byte)0x3B;
    private short storeDataLen; // Size of data stored inside bufferArray

    public void initToolkit()
    {
        // Registring to the Toolkit
        tkRegistry = ToolkitRegistrySystem.getEntry();

        fvDF_PB_TELECOM = UICCSystem.getTheUICCView(JCSystem.NOT_A_TRANSIENT_OBJECT);
        fvDF_PB_TELECOM.select(UICCConstants.FID_DF_TELECOM);
        fvDF_PB_TELECOM.select(FID_DF_PHONEBOOK);

        // Registering to the File Update Event to monitor Phonebook under DF Telecom
        tkRegistry.registerFileEvent(ToolkitConstants.EVENT_EXTERNAL_FILE_UPDATE, fvDF_PB_TELECOM);

        try
        {
            fvDF_PB_ADF = UICCSystem.getTheFileView(usim_AID, (short)0, (byte)usim_AID.length, JCSystem.NOT_A_TRANSIENT_OBJECT);
            fvDF_PB_ADF.select(FID_DF_PHONEBOOK);
            // Registering to the FIle Update event to monitor Phonebook under ADF USIM (if any present)
            tkRegistry.registerFileEvent(ToolkitConstants.EVENT_EXTERNAL_FILE_UPDATE, fvDF_PB_ADF);
        }
        catch (Exception ignored)
        {
            // In this case, there is no DF Phonebook under ADF thus, no exception should be handled
            // as the DF Phonebook under the USIM is optional
        }

        // The editHandler is used to buffer the file update event in case of reentrancy
        editHandler = (EditHandler)HandlerBuilder.buildTLVHandler(HandlerBuilder.EDIT_HANDLER, (short)100);
    }

    public static void install (byte[] bArray, short bOffset, byte bLength)
    {
        PhoneBookMonitorApplet applet = new PhoneBookMonitorApplet();
        applet.register();
        applet.initToolkit();
    }

    @Override
    public void processToolkit(short event) throws ToolkitException
    {
        byte[] globalBuffer;
        // The Volative Byte Array is retrieved to reduce resosurce consumption
        globalBuffer = UICCPlatform.getTheVolatileByteArray();
        short dataLen;

        if (ToolkitConstants.EVENT_EXTERNAL_FILE_UPDATE == event)
        {
            editHandler.clear();
            EnvelopeHandler envHdlr = EnvelopeHandlerSystem.getTheHandler();
            
            envHdlr.findTLV(ToolkitConstants.TAG_FILE_LIST, (byte)0x01);
            dataLen = envHdlr.copyValue((short)0, globalBuffer, (short)0, envHdlr.getValueLength());
            editHandler.appendTLV(ToolkitConstants.TAG_FILE_LIST, globalBuffer, (short)0, dataLen);

            if (envHdlr.findTLV(ToolkitConstants.TAG_AID, (byte)0x01) != ToolkitConstants.TLV_NOT_FOUND)
            {
                dataLen = envHdlr.copyValue((short)0, globalBuffer, (short)0, envHdlr.getValueLength());
                editHandler.appendTLV(ToolkitConstants.TAG_AID, globalBuffer, (short)0, dataLen);
            }
            envHdlr.findTLV(TAG_UPDATE_INFO, (byte)0x01);
            dataLen = envHdlr.copyValue((short)0, globalBuffer, (short)0, dataLen);
            editHandler.appendTLV(TAG_UPDATE_INFO, globalBuffer, (short)0, dataLen);

            // To send out the SMS, the Proactive Handler must be available.
            // In case it isn't, the application registers to the Proactive Handler Available Event and quits.
            try
            {
                ProactiveHandler proHdlr = ProactiveHandlerSystem.getTheHandler();
            }catch(Exception e)
            {
                // The EnvelopeHandler information is stored inside the editHandler, to be used at the Proactive Handler Event afterward.
                // Note: if two subsequent File Update Event happend in reentrance, the first one is lost
                tkRegistry.setEvent(ToolkitConstants.EVENT_PROACTIVE_HANDLER_AVAILABLE);
                return;
            }
            sendSMS();
        }

        if (ToolkitConstants.EVENT_PROACTIVE_HANDLER_AVAILABLE == event)
        {
            // Registration to this event is only available after the triggering in reentrance and eventual availability
            // of the Proactive Handler
            sendSMS();
        }
    }

    public void sendSMS()
    {
        byte[] globalBuffer;
        short offset = (short)0;

        globalBuffer = UICCPlatform.getTheVolatileByteArray();
        ProactiveHandler proHdlr = ProactiveHandlerSystem.getTheHandler();
        proHdlr.init(PRO_CMD_SMS, (byte)0x00, ToolkitConstants.DEV_ID_NETWORK);

        //Build the TP-DU
        globalBuffer[offset++] = (byte)0x01;
        globalBuffer[offset++] = (byte)0x01;
        offset = Util.arrayCopyNonAtomic(destAddress, (short)0, globalBuffer, offset, (short)destAddress.length);

        globalBuffer[offset++] = (byte)0x7F;
        globalBuffer[offset++] = (byte)0xF6;
        globalBuffer[offset++] = (byte)editHandler.getLength();
        offset = editHandler.copy(globalBuffer, offset, editHandler.getLength());

        //Total length of TPDU
        proHdlr.appendTLV((byte)(PRO_CMD_SMS_TPDU_TAG | ToolkitConstants.TAG_SET_CR), globalBuffer, (short)0, offset);
        proHdlr.send();
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
