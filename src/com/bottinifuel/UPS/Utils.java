/*
 * Created on Sep 15, 2006 by pladd
 *
 */
package com.bottinifuel.UPS;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class Utils
{
    static final int UNKNOWN_REGION  = 0;
    static final int OIL_REGION      = 1;
    static final int PROPANE_REGION  = 3;
    static final int GAS_REGION      = 4;
 
    // Data Index numbers for ADD CSV file
    // All parameters are output data unless specified otherwise
    static final int ADD_CSV_CUST_NUM         =  0; // <-- Inbound data
    static final int ADD_CSV_TANK_NUM         =  1; // <-- Inbound data
    static final int ADD_CSV_ORDER_NUM        =  2; // <-- Inbound data
    static final int ADD_CSV_CUST_NAME        =  3;
    static final int ADD_CSV_ADDR1            =  4;
    static final int ADD_CSV_ADDR2            =  5;
    static final int ADD_CSV_CITY             =  6;
    static final int ADD_CSV_STATE            =  7;
    static final int ADD_CSV_ZIP              =  8;
    static final int ADD_CSV_TANK_SIZE        =  9;
    static final int ADD_CSV_PRODUCT_CODE     = 10;
    static final int ADD_CSV_EXPECTED_DROP    = 11;
    static final int ADD_CSV_IDEAL_DROP       = 12;
    static final int ADD_CSV_WILL_CALL_FLAG   = 13;
    static final int ADD_CSV_CUST_REQ_FLAG    = 14;
    static final int ADD_CSV_PHONE_ORD_FLAG   = 15; // <-- Inbound data?
    static final int ADD_CSV_PHONE_ORD_GAL    = 16; // <-- Inbound data?
    static final int ADD_CSV_PHONE_ORD_DATE   = 17;
    static final int ADD_CSV_MUST_DO_FLAG     = 18;
    static final int ADD_CSV_DAY_OF_WEEK_FLAG = 19;
    static final int ADD_CSV_DUE_DATE         = 20;
    static final int ADD_CSV_SCHED_DATE       = 21;
    static final int ADD_CSV_RUNOUT_DATE      = 22;
    static final int ADD_CSV_DELIVERY_NOTIF   = 23;
    static final int ADD_CSV_ZONE_NUM         = 24;
    static final int ADD_CSV_DELIVERY_SEQ     = 25;
    static final int ADD_CSV_LATITUDE         = 26;
    static final int ADD_CSV_LONGITUDE        = 27;
    static final int ADD_CSV_DRIVER_NUM       = 28; // <-- Inbound data
    static final int ADD_CSV_DELIVERY_DATE    = 29; // <-- Inbound data 
    static final int ADD_CSV_SEQUENCE_NUM     = 30; // <-- Inbound data
    static final int ADD_CSV_PRIORITY         = 31;
    static final int ADD_CSV_FILL_LOC         = 32;
    static final int ADD_CSV_DIVISION         = 33;
    static final int ADD_CSV_COMMENT          = 34;
    static final int ADD_CSV_SNOW_DROP        = 35;
    static final int ADD_CSV_DELIVERY_GROUP   = 36;
    static final int ADD_CSV_ZONE_DESC        = 37;
    static final int ADD_CSV_DEL_CENTER_NUM   = 38;
    static final int ADD_CSV_LAST_DEL_DATE    = 39;
    static final int ADD_CSV_LAST_DEL_GAL     = 40;
    
    private static final String OrderFileFormat =
                                               // Field #  Start ADD Field           Dir  UPS Field
                                               // ==========================================
        "{fill:width=1,pad=;}"               +
        "{text:width=10,align=right,pad= } " + // 4/6      23    Expected Drop        ->  Size 1,1
        "{fill:width=1,pad=;}"               +
        "{text:width=10,align=right,pad= } " + // 5/8      34    Expected Drop        ->  Size 1,2
        "{fill:width=1,pad=;}"               +
        "{text:width=10,align=right,pad= } " + // 6/10     45    Expected Drop        ->  Size 1,3
        "{fill:width=1,pad=;}"               +
        "{text:width=10,align=right,pad= } " + // 7/12     56    Max Drop (tank size) ->  Size 2,1
        "{fill:width=1,pad=;}"               +
        "{text:width=10,align=right,pad= } " + // 8/14     67    Max Drop (tank size) ->  Size 2,2
        "{fill:width=1,pad=;}"               +
        "{text:width=10,align=right,pad= } " + // 9/16     78    Max Drop (tank size) ->  Size 2,3
        "{fill:width=1,pad=;}"               +
        "{text:width=10,align=right,pad= } " + // 10/18    89    Weight               ->  Size 3,1
        "{fill:width=1,pad=;}"               +
        "{text:width=10,align=right,pad= } " + // 11/20    100   Weight               ->  Size 3,2
        "{fill:width=1,pad=;}"               +
        "{text:width=10,align=right,pad= } " + // 12/22    111   Weight               ->  Size 3,3
        "{fill:width=1,pad=;}"               +
        "{text:width=1,align=right,pad= } "  + // 13/24    122   Phone Order          ->  User Def Ord 1
        "{fill:width=1,pad=;}"               +
        "{text:width=1,align=right,pad= } "  + // 14/26    124   Order Selector       ->  ???
        "{fill:width=1,pad=;}"               +
        "{date:pattern=yyyyMMdd} "           + // 15/28    126   Sched Date           ->  Ord Begin Date
        "{fill:width=1,pad=;}"               + 
        "{date:pattern=yyyyMMdd} "           + // 16/30    135   Runout Date          ->  Ord End Date
        "{fill:width=1,pad=;}"               + 
        "{text:width=3,pad= } "              + // 17/32    144   Priority             ->  Priority
        "{fill:width=1,pad=;}"               + 
        "{text:width=255,pad= } "            + // 18/34    148   Order Comment        ->  Special Instructions
        "{fill:width=1,pad=;}"               + 
        "{date:pattern=yyyyMMdd} "           + // 19/36    404   Last Delivery Date   ->  User Def Ord 2
        "{fill:width=1,pad=;} "              + 
        "{text:width=8,pad= } "              + // 20/38    413   Last Delivery Units  ->  User Def Ord 3

        // Returned data from UPS
        "{fill:width=1,pad=;} "              + 
        "{text:width=15,pad= } "             + // 21/40    422   N/A                  <-  Route ID
        "{fill:width=1,pad=;} "              + 
        "{text:width=4,pad= } "              + // 22/42    438   Sequence Number      <-  Stop Number
        "{fill:width=1,pad=;} "              + 
        "{text:width=30,pad= } "             + // 23/44    443   N/A                  <-  Route Name
        "{fill:width=1,pad=;} "              + 
        "{text:width=9,pad= } "              + // 24/46    474   Driver #             <-  Driver ID 
        "{fill:width=1,pad=;} "              + 
        "{text:width=20,pad= } "             + // 25/48    484   Truck #              <-  Equipment ID
        "{fill:width=1,pad=;} "              + 
        "{text:width=8,pad= } "              ; // 26/50    505   Delivery Date        <-  Delivery Date 
    
    public static final String OrderFileFormatRead =
                                               // Field #  Start ADD Field           Dir  UPS Field
                                               // ==========================================
        "{text:width=7,align=right,pad=@} "  + // 1/0      0     Cust #               ->  Location ID
        "{fill:width=1,pad=;}"               +
        "{text:width=3,align=right,pad=@} "  + // 2/2      8     Tank #               ->  Location Type
        "{fill:width=1,pad=;}"               +
        "{text:width=10,align=right,pad=@} " + // 3/4      12    Order #              ->  ???
        OrderFileFormat;
        
    public static final String OrderFileFormatWrite =
                                               // Field #  Start ADD Field           Dir  UPS Field
                                               // ==========================================
        "{text:width=7,align=right,pad= } "  + // 1/0      0     Cust #               ->  Location ID
        "{fill:width=1,pad=;}"               +
        "{text:width=3,align=right,pad= } "  + // 2/2      8     Tank #               ->  Location Type
        "{fill:width=1,pad=;}"               +
        "{text:width=10,align=right,pad= } " + // 3/4      12    Order #              ->  ???
        OrderFileFormat;
    
    
    public static final String LocationFileFormat =
        "{text:width=7,align=right,pad= } " + // Cust #    -> Location ID
        "{fill:width=1,pad=;} "             +
        "{text:width=3,align=right,pad= } " + // Tank #    -> Location Type
        "{fill:width=1,pad=;} "             +
        "{text:width=58,pad= } "            + // Name
        "{fill:width=1,pad=;} "             +
        "{text:width=40,pad= } "            + // Street 1
        "{fill:width=1,pad=;} "             +
        "{text:width=15,pad= }"             + // Fill Loc  -> Street 2
        "{fill:width=1,pad=;} "             +
        "{text:width=38,pad= }"             + // City
        "{fill:width=1,pad=;} "             +
        "{text:width=20,pad= }"             + // County
        "{fill:width=1,pad=;} "             +
        "{text:width=2,pad= } "             + // State
        "{fill:width=1,pad=;} "             +
        "{text:width=9,pad= } "             + // Zip
        "{fill:width=1,pad=;} "             +
        "{text:width=10,pad= } "            + // Telephone
        "{fill:width=1,pad=;} "             +
        "{text:width=5,pad= } "             + // Type      -> Cust Type
        "{fill:width=1,pad=;} "             +
        "{text:width=3,align=right,pad= } " + // Product   -> User Def 1
        "{fill:width=1,pad=;} "             +
        "{text:width=6,align=right,pad= } " + // Tank Size -> User Def 2
        "{fill:width=1,pad=;} "             +
        "{text:width=255,pad= } "           + // Del Instr
        "{fill:width=1,pad=;} "             +
        "{text:width=10,pad= } "            + // Del Cent  -> Zone
        "{fill:width=1,pad=;} "             +
        "{text:width=10,pad= } "            + // Del Zone  -> Preferred Route 
        "{fill:width=1,pad=;} "             +
        "{text:width=5,pad= } "             + // Del Group -> User Def 3
        "{fill:width=1,pad=;} "             ;
        

    
    public static int ProductToUPSRegion(int product)
    {
        switch (product)
        {
        case 2:  // #2 Oil
        case 9: // Kerosene
        case 11: // Winter Blend
        case 20: // Dyed DSL
            return OIL_REGION;
        
        case 6: // Metered Propane 
        case 7: // Propane
        case 8: // Propane Cylinder
            return PROPANE_REGION;

        case 10: // Clear Kerosene
        case 19: // Clear DSL
        case 21: // Clear DSL no tax
        case 22: // B20 Diesel
        case 15: // NL Gas
        case 25: // NL Gas Summer
        case 16: // Mid Grade Gas
        case 26: // Mid Grade Gas Summer
        case 18: // Prem Gas
        case 28: // Prem Gas Summer
            return GAS_REGION;

        default:
            return UNKNOWN_REGION;
        }
    }

    
    public static String TaxJurToCountyName(int tax_jur) 
    {
        switch (tax_jur)
        {
        case 1:
        case 2:  return "DUTCHESS";
        case 3:  return "PUTNAM";
        case 4:  return "WESTCHESTER";
        case 5:  return "SULLIVAN";
        case 22:
        case 23:
        case 24:
        case 6:  return "ORANGE";
        case 21:
        case 7:  return "COLUMBIA";
        case 8:
        case 14: return "ULSTER";
        case 9:  return "NJ";
        case 10: return "PA";
        case 11: return "ROCKLAND";
        case 12: return "GREENE";
        case 13: return "CT";
        case 15:
        case 20: return "ALBANY";
        case 16:
        case 17: return "DELAWARE";
        case 18:
        case 19: return "SCHOHARIE";
        case 25: return "MA";
        default:
            return null;
        }
    }


    public static String AcctTypeToUPSTypeName(int type)
    {
        switch (type)
        {
        case 1:  return "RESID";
        case 2:  return "COMM";
        case 3:  return "AGRI";
        case 4:  return "BIDS";
        case 5:  return "ASST";
        case 6:  return "MONTH";
        case 7:  return "TERM";
        default:
            return null;
        }
    }

    
    public static String DelCtrToDelCtrName(int del_ctr)
    {
        switch (del_ctr)
        {
        case 1:  return "WAPP_DIST";  
        case 2:  return "PJ_DIST";         
        case 3:  return "WV_DIST";
        case 4:  return "RH_DIST";
        case 5:  return "WAPP_PRO";
        case 6:  return "WAPP_G&D";    
        case 7:  return "WV_PRO";   
        case 8:  return "PJ_PRO"; 
        case 9:  return "WASH_G&D";
        case 10: return "PJ_CDSL";
        case 11: return "WASH_BIG";
        case 12: return "COLONIAL";
        case 13: return "COL_G&D";
        case 14: return "TANN";
        case 15: return "TANN_PRO";
        case 16: return "CAIRO";
        case 17: return "MARG";
        case 18: return "SAUG";
        case 19: return "SAUG_PRO";
        case 20: return "KHK";
        case 21: return "KHK_PRO";
        case 22: return "PB_DIST";
        case 23: return "VALLEY";
        default: 
            return null;  
        }
    }

    
    public static String LocationQuery(String criteria)
    {
        return 
        "SELECT dbo.FULL_ACCOUNT.full_account, dbo.TANKS.tank_num, dbo.TANKS.fill_location, " +
            "dbo.DAD_TEXT.dad_title, dbo.DAD_TEXT.dad_first_name, dbo.DAD_TEXT.dad_middle_initial, " +
            "dbo.DAD_TEXT.dad_last_name, dbo.DAD_TEXT.dad_name_suffix, " +
            "dbo.DAD_TEXT.dad_street1, dbo.DAD_TEXT.dad_street2, " +
            "dbo.DAD_TEXT.dad_city, dbo.DAD_TEXT.dad_state, dbo.DAD_TEXT.dad_postal_code, " +
            "dbo.ACCOUNTS.telephone, dbo.TANKS.tax_jurisdiction, dbo.TANKS.product, dbo.TANKS.size, " +
            "dbo.DIN_TEXT.din_text, dbo.ACCOUNTS.type, dbo.DELIVERY_CENTER_ZONE.delivery_center_id, " +
            "dbo.TANKS.zone, dbo.TANKS.delivery_group " +
        "FROM ((((((dbo.ACCOUNTS INNER JOIN dbo.FULL_ACCOUNT ON dbo.ACCOUNTS.account_num = dbo.FULL_ACCOUNT.account_num) " +
            "INNER JOIN dbo.TANKS ON dbo.ACCOUNTS.account_num = dbo.TANKS.account_num) " +
            "INNER JOIN dbo.DELIVERY_ZONE ON dbo.TANKS.zone = dbo.DELIVERY_ZONE.description) " +
            "INNER JOIN dbo.DELIVERY_CENTER_ZONE on dbo.DELIVERY_ZONE.zone_id = dbo.DELIVERY_CENTER_ZONE.zone_id) " +
            "INNER JOIN dbo.DAD_TEXT ON dbo.TANKS.tank_seq_number = dbo.DAD_TEXT.dad_text_owner) " +
            "LEFT  JOIN dbo.DIN_TEXT ON dbo.TANKS.tank_seq_number = dbo.DIN_TEXT.din_text_owner) " +
        "WHERE dbo.ACCOUNTS.terminated='N' " +
            "AND (dbo.TANKS.tank_status NOT IN ('T', 'I') OR " +
            "     dbo.TANKS.tank_status IS NULL) " +
            "AND dbo.TANKS.product <> 0 " +
            "AND dbo.TANKS.delivery_stop Not In (5,10,11) " +
            "AND dbo.ACCOUNTS.division<>63 " +
            criteria + " " +
        "ORDER BY dbo.ACCOUNTS.account_num, dbo.TANKS.tank_num";
    }
    
    public static void ZipToFile(String infile, String outdir, String outfile) throws Exception
    {
        try {
            FileOutputStream dest = new FileOutputStream(outdir + "\\" + outfile + ".zip");

            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            out.setMethod(ZipOutputStream.DEFLATED);

            byte data[] = new byte[2048];
            FileInputStream fi = new FileInputStream(infile);
            BufferedInputStream origin = new BufferedInputStream(fi, 2048);
            
            ZipEntry entry = new ZipEntry(outfile + ".txt");
            out.putNextEntry(entry);
            
            int count;
            while ((count = origin.read(data, 0, 2048)) != -1)
            {
                out.write(data, 0, count);
            }
            
            origin.close();
            out.close();
        }
        catch (Exception e)
        {
            System.out.println(e);
            throw e;
        }
    }
    
    public static void main(String[] args)
    {
        for (int i = 1; i <= 24; i++)
            System.out.println("TJ " + i + " = " + Utils.TaxJurToCountyName(i));
    }

    public static final double OIL_WT_PER_GAL = 7.0;
    public static final double PRO_WT_PER_GAL = 4.2;
    public static final double GAS_WT_PER_GAL = 6.2;
    public static final double DSL_WT_PER_GAL = 6.7;
    public static final double KER_WT_PER_GAL = 6.7;
}
