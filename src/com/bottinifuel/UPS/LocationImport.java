/*
 * Created on Sep 13, 2006 by pladd
 *
 */
package com.bottinifuel.UPS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bottinifuel.Energy.Info.InfoFactory;
import com.ribomation.fixedwidthfield.Formatter;

/**
 * @author pladd
 *
 */
public class LocationImport
{
    private boolean ParmsError;
    private boolean Interactive;
    
    // *FIXME*
    private String ParmsFileName     = "S:/Delivery/UPS_Logistics/data/LocationImportParms.txt";
    private String TestParmsFileName = "S:/Delivery/UPS_Logistics/data/TestLocationImportParms.txt";
    private Date   LastImportDate;
    private String LastImport;
    private String LastUser = "";
    static private final String DTFormat = "yyyy-MM-dd HH:mm:ss";
    static private DateFormat DateFormat = new SimpleDateFormat(DTFormat);

    private String EnergyHost    = "";
    private int    EnergyPortInt = 0;
    private String EnergyPort    = "";
    private String EnergyDB      = "";
    private String EnergyUser    = "";
    private String EnergyPW      = "";

    private InfoFactory EnergyConn;
    
    private File OutDir;
    private File OilFile;
    private File ProFile;
    private File GasFile;

    private FileOutputStream OilStream;
    private FileOutputStream ProStream;
    private FileOutputStream GasStream;

    private Date ImportDate;
    private String ImportUser;

    private void InitParms(boolean test) throws Exception
    {
        Attributes attrs;
        try {
            Manifest lastUpdate = new Manifest(new FileInputStream(test ? TestParmsFileName : ParmsFileName));
            attrs = lastUpdate.getMainAttributes();
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Error opening last import information: " + e);
            ParmsError = true;
            LastImportDate = new Date(0);
            return;
        }

        String [] varNames = { "LastImport", "LastUser", "EnergyHost", "EnergyPort", "EnergyDB", "EnergyUser", "EnergyPW" }; 
        for (String key : varNames)
        {
            if (attrs.containsKey(new Attributes.Name(key)))
            {
                try
                {
                    this.getClass().getDeclaredField(key).set(this, attrs.getValue(key));
                }
                catch (NoSuchFieldException e)
                {
                    System.out.println("Error: Missing variable for field \"" + key + "\"");
                }
                catch (Exception e)
                {
                    throw e;
                }
            }
            else
            {
                System.out.println("Missing attribute \"" + key + "\"");
                ParmsError = true;
            }
        }

        try {
            LastImportDate = DateFormat.parse(LastImport);
        }
        catch (ParseException e)
        {
            System.out.println("Error date parsing LastImport attribute: " + e);
            ParmsError = true;
            LastImportDate = new Date(0);
        }
        
        try {
            EnergyPortInt = Integer.parseInt(EnergyPort);
        }
        catch (NumberFormatException e)
        {
            System.out.println("Error integer parsing EnergyPort attribute: " + e);
            ParmsError = true;
        }
    }
    
    
    private void WriteParms(boolean test) throws Exception
    {
        OutputStream os; 
        try {
            os = new FileOutputStream(test ? TestParmsFileName : ParmsFileName);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Error: can open ParmsFile for saving");
            return;
        }

        os.write("LastUser: ".getBytes());
        if (!Interactive)
            os.write(ImportUser.getBytes());
        else
            os.write(LastUser.getBytes());
        os.write('\n');
        
        os.write("LastImport: ".getBytes());
        if (!Interactive)
            os.write(DateFormat.format(ImportDate).getBytes());
        else
            os.write(DateFormat.format(LastImportDate).getBytes());
        os.write('\n');
        
        os.write("EnergyHost: ".getBytes());
        os.write(EnergyHost.getBytes());
        os.write('\n');

        os.write("EnergyPort: ".getBytes());
        os.write(EnergyPort.getBytes());
        os.write('\n');

        os.write("EnergyDB: ".getBytes());
        os.write(EnergyDB.getBytes());
        os.write('\n');

        os.write("EnergyUser: ".getBytes());
        os.write(EnergyUser.getBytes());
        os.write('\n');

        os.write("EnergyPW: ".getBytes());
        os.write(EnergyPW.getBytes());
        os.write('\n');
        
        os.close();
    }

    
    public LocationImport(String outDir, boolean test) throws Exception
    {
        DateFormat.setLenient(false);
        InitParms(test);
        System.out.println("Last Import User: " + LastUser);

        // Check output directory
        OutDir = new File(outDir);
        if (!OutDir.isDirectory())
        {
            System.out.println("Error: Output location is not a directory: " + OutDir);
            throw new Exception();
        }

        // Open output directories
        OilFile = new File(OutDir.getAbsolutePath() + File.separator + "locations_oil.txt");
        try { OilStream = new FileOutputStream(OilFile); }
        catch (FileNotFoundException e) {
            System.out.println("Error: Can't create oil output file: " + OilFile);
        }
        
        ProFile = new File(OutDir.getAbsolutePath() + File.separator + "locations_pro.txt");
        try { ProStream = new FileOutputStream(ProFile); }
        catch (FileNotFoundException e) {
            System.out.println("Error: Can't create Propane output file: " + ProFile);
        }
        
        GasFile = new File(OutDir.getAbsolutePath() + File.separator + "locations_gas.txt");
        try { GasStream = new FileOutputStream(GasFile); }
        catch (FileNotFoundException e) {
            System.out.println("Error: Can't create Gas output file: " + GasFile);
        }
        
        int count = 0;
        int oilLocations = 0;
        int proLocations = 0;
        int gasLocations = 0;

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Review/change Energy login parms? [n]:");
        String reviewLogin = in.readLine();
        if (ParmsError || reviewLogin != null && reviewLogin.equals("y") || reviewLogin.equals("Y"))
        {
            System.out.print("ADD Energy Host  [" + EnergyHost  + "]:");
            String newHost = in.readLine();
            if (newHost != null && newHost.length() != 0)
                EnergyHost = newHost;

            System.out.print("ADD Energy Port  [" + EnergyPort  + "]:");
            String newPort = in.readLine();
            if (newPort != null && newPort.length() != 0)
            {
                EnergyPort = newPort;
                EnergyPortInt = Integer.parseInt(EnergyPort);
            }

            System.out.print("ADD Energy DB    [" + EnergyDB    + "]:");
            String newDB   = in.readLine();
            if (newDB != null && newDB.length() != 0)
                EnergyDB = newHost;

            System.out.print("ADD Energy Login [" + EnergyUser + "]:");
            String newUser = in.readLine();
            if (newUser != null && newUser.length() != 0)
                EnergyUser = newUser;

            System.out.print("ADD Energy Pass  [" + EnergyPW    + "]:");
            String newPW   = in.readLine();
            if (newPW != null && newPW.length() != 0)
                EnergyPW = newHost;
        }

        Statement s;
        try {
            EnergyConn = new InfoFactory(EnergyHost, EnergyPortInt, EnergyDB, EnergyUser, EnergyPW);
            s = EnergyConn.getStatement();
        }
        catch (Exception e)
        {
            System.out.println("Error opening Energy Connection: " + e);
            throw e;
        }

        System.out.print("Automatic import? [y]:");
        String automatic = in.readLine();
        if (automatic != null && automatic.equals("n") || automatic.equals("N"))
            Interactive = true;
        else
            Interactive = false;

        String queryConstraints;
        int interactiveCount = 0;
        if (!Interactive)
        {
            boolean done = false;
            do
            {
                System.out.print("Retrieve changes since [" + LastImport + "]:");
                String newDate = in.readLine();
                if (newDate != null && newDate.length() != 0)
                {
                    LastImport = newDate;
                    try {
                        LastImportDate = DateFormat.parse(LastImport);
                        done = true;
                    }
                    catch (ParseException e)
                    {
                        System.out.println("Error date parsing LastImport attribute: " + e);
                        LastImportDate = new Date(0);
                    }
                }
                else
                    done = true;
            }
            while (!done);

            Timestamp sqlDate = new Timestamp(LastImportDate.getTime());
            queryConstraints  = "AND (dbo.DAD_TEXT.dad_last_maintenance_dt > '" + sqlDate + "' " +
                "OR dbo.DIN_TEXT.din_last_maintenance_dt > '" + sqlDate + "') ";
        }
        else
        {
            Vector<Integer> accts = new Vector<Integer>();
            Vector<Integer> tanks = new Vector<Integer>();

            System.out.print("Import missing locations? [y]:");
            String missingLocQ = in.readLine();
            if (missingLocQ != null && missingLocQ.equals("n") || missingLocQ.equals("N"))
            {
                boolean done = false;
                while (!done)
                {
                    String acct = "";
                    boolean done2 = false;
                    while (!done2 && !done)
                    {
                        try {
                            System.out.print("Acct #:");
                            acct = in.readLine();
                            if (acct != null && acct.length() != 0)
                            {
                                int acctInt = EnergyConn.AccountNum(Integer.parseInt(acct));
                                accts.addElement(new Integer(acctInt));
                                done2 = true;
                            }
                            else
                            {
                                done = true;
                                continue;
                            }
                        }
                        catch (Exception e)
                        {
                            System.out.println(e);
                            continue;
                        }
                    }

                    done2 = false;
                    while (!done && !done2)
                    {
                        try {
                            System.out.print("Tank #:");
                            String tank = in.readLine();
                            if (tank != null && tank.length() != 0)
                            {
                                tanks.addElement(new Integer(tank));
                                done2 = true;
                            }
                        }
                        catch (Exception e)
                        {
                            System.out.println(e);
                        }
                    }
                }
            }
            else
            {
                File ErrorFile = new File("C:/Program Files/UPSLT/Config/ImportOrderErrors.log");
                if (!ErrorFile.exists())
                	ErrorFile = new File("C:/UPSLT/Client/Config/ImportOrderErrors.log");
                LineNumberReader errors;
                try {
                    errors = new LineNumberReader(new InputStreamReader(new FileInputStream(ErrorFile)));

                    Pattern linePat = Pattern.compile("Warning: Order \"\\d+\\\" contains a missing location \\((\\d+)/(\\d+)\\)");
                    Matcher lineMatcher = linePat.matcher("");

                    String line = errors.readLine();
                    while (line != null)
                    {
                        lineMatcher.reset(line);
                        if (lineMatcher.matches())
                        {
                            int acct = Integer.parseInt(lineMatcher.group(1));
                            int tank = Integer.parseInt(lineMatcher.group(2));
                            accts.add(acct / 10);
                            tanks.add(tank);
                            System.out.println("Importing location " + acct + "/" + tank);
                        }
                        else if (line.length() > 0 && errors.getLineNumber() != 1)
                            if (line.length() < 17 || !line.substring(0, 17).equals("------ LOG END : "))
                                System.out.println("Skipping unrecognized line #" + errors.getLineNumber() + " - " + line);
                        line = errors.readLine();
                    }
                    
                    System.out.println("\nFound " + accts.size() + " missing locations to import\n");
                }
                catch (FileNotFoundException e) {
                    System.out.println("Error: Can't open ErrorFile file: " + ErrorFile);
                }
                
                interactiveCount = accts.size();
            }
            
            if (accts.size() <= 0)
                return;

            queryConstraints = "AND (";
            for (int i = 0; i < accts.size(); i++)
            {
                if (i > 0)
                    queryConstraints += " OR ";
                queryConstraints += " (dbo.TANKS.account_num = " + accts.get(i) + " AND dbo.TANKS.tank_num = " + tanks.get(i) + ") ";
            }   
            queryConstraints += ") ";
        }

        ImportDate = new Date();
        ImportUser = System.getProperty("user.name");

        int acct = 0;
        int tank = 0;
        try {            
            String query = Utils.LocationQuery(queryConstraints);
            ResultSet r = s.executeQuery(query);

            Formatter outFmt = new Formatter();
            outFmt.addFwfClass("date", com.ribomation.fixedwidthfield.formatter.DateFWF.class);
            outFmt.setFields(Utils.LocationFileFormat);

            while (r.next())
            {
                count++;
                       acct   = r.getInt("full_account");
                       tank   = r.getInt("tank_num");
                String fill   = r.getString("fill_location");
                String str1   = r.getString("dad_street1");
                String str2   = r.getString("dad_street2");
                String city   = r.getString("dad_city");
                String state  = r.getString("dad_state");
                String zip    = r.getString("dad_postal_code");
                String phone  = r.getString("telephone");
                int    type   = r.getInt("type");
                int    prod   = r.getInt("product");
                int    size   = r.getInt("size");
                String din    = r.getString("din_text");
                @SuppressWarnings("unused")
				int    delCtr = r.getInt("delivery_center_id");
                String zone   = r.getString("zone");
                int    delGrp = r.getInt("delivery_group");

                String delGroup;
                if (delGrp == 0)
                    delGroup = "";
                else
                    delGroup = Integer.toString(delGrp);

                String title  = r.getString("dad_title");
                String firstn = r.getString("dad_first_name");
                String midini = r.getString("dad_middle_initial");
                String lastn  = r.getString("dad_last_name");
                String suffix = r.getString("dad_name_suffix");

                fill = "<>" + fill.trim().toUpperCase() + "<>";

                if (Interactive)
                    System.out.println("Updating location " + acct + "/" + tank);

                String name = "";
                boolean prev = false;
                if (title  != null && !title.equals("    ")) {
                    name += title.trim();
                    prev = true;
                }
                if (firstn != null && !firstn.equals(" "))   {
                    if (prev) name += " ";
                    name += firstn.trim();
                    prev = true;
                }
                if (midini != null && !midini.equals(" "))   {
                    if (prev) name += " ";
                    name += midini.trim();
                    prev = true;
                }
                if (lastn  != null && !lastn.equals(" "))    {
                    if (prev) name += " ";
                    name += lastn.trim();
                    prev = true;
                }
                if (suffix != null && !suffix.equals("   ")) {
                    if (prev) name += " ";
                    name += suffix.trim();
                    prev = true;
                }
                
                String county = Utils.TaxJurToCountyName(r.getInt("tax_jurisdiction"));
                if (county == null)
                {
                    System.out.println( "Acct# " + acct + "  - Invalid tax jurisdiction: " + r.getInt("tax_jurisdiction"));
                    county = "";
                }
                String acctType = Utils.AcctTypeToUPSTypeName(type);
                if (acctType == null)
                {
                    System.out.println("Acct #" + acct + " - Unknown acct type: " + type);
                    acctType = "";
                }

                String delCtrName = Utils.DelCtrToDelCtrName(r.getInt("delivery_center_id"));
                if (delCtrName == null) 
                {
                    System.out.println("Acct #" + acct + " - Unknown del center: " + r.getInt("delivery_center_id"));
                    delCtrName = "";
                }

                String delInst = "";
                boolean hasAddr2 = false;
                if (str2 != null)
                    str2 = str2.trim();
                if (str2 != null && str2.length() > 0)
                {
                    delInst += str2.trim();
                    hasAddr2 = true;
                }
                
                if (din != null)
                    din = din.trim();
                
                if (din != null && din.length() > 0)
                {
                    if (hasAddr2)
                        delInst += " / " + din;
                    else
                        delInst = din;
                }
                
                delInst = delInst.toUpperCase();

                if (delInst.length() > 255)
                    System.out.println("Warning: " + acct + "/" + tank + " delivery instruct > 255 chars, truncated: (" +
                                       delInst.length() + ") " + delInst.substring(0, 254) + "|" + delInst.substring(254));
                if (city.length() > 30)
                    System.out.println("Warning: " + acct + "/" + tank + " city > 30 chars, truncated: (" +
                                       city.length() + ") " + city.substring(0, 29) + "|" + city.substring(29));
                
                Object [] outLine = { acct,                null, tank,               null, name.toUpperCase(),    null, 
                                      str1.toUpperCase(),  null, fill.toUpperCase(), null, 
                                      city.toUpperCase(),  null, county,             null,
                                      state.toUpperCase(), null, zip,                null,
                                      phone,               null, acctType,           null,
                                      prod,                null, size,               null, delInst.toUpperCase(), null,
                                      delCtrName,          null, zone,               null, delGroup, null};
                String out = outFmt.format(outLine);

                switch (Utils.ProductToUPSRegion(prod))
                {
                case Utils.OIL_REGION:
                    oilLocations++;
                    OilStream.write(out.getBytes());
                    OilStream.write('\n');
                    break;
                case Utils.PROPANE_REGION:
                    proLocations++;
                    ProStream.write(out.getBytes());
                    ProStream.write('\n');
                    break;
                case Utils.GAS_REGION:
                    gasLocations++;
                    GasStream.write(out.getBytes());
                    GasStream.write('\n');
                    break;
                case Utils.UNKNOWN_REGION:
                default:
                    System.out.println("Unknown product: " + prod + " Acct #" + acct);
                    break;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Error processing locations: working on acct #" + acct + " tank #" + tank + " - " + e);
        }

        System.out.println("\nLocations Processed:" +
                           "\n Total:   " + count +
                           "\n Oil:     " + oilLocations +
                           "\n Propane: " + proLocations + 
                           "\n Gas:     " + gasLocations);
        if ((oilLocations + proLocations + gasLocations) != count)
            System.out.println("Error: Mismatch in total location count!");

        if (Interactive && count != interactiveCount)
            System.out.println("\nError: Import count does not match export count");
        
        WriteParms(test);
    }

    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // v1.4  : added <><> Fill Location eyecatchers
        // v1.5  : check last_maint_date on DIN_TEXT table also
        // v1.9  : Moved winter blend & dyed diesel to Oil Region
        // v1.10 : Import exceptions file automatically
        // v1.11 : Add delivery group as User Def field 3
        // v1.12 : Add new tax jur, del centers
        //       : Messages for invalid jurs, del ctrs, products
        // v1.13 : Output acct info for invalid jurs, del ctrs, products
        // v1.14 : New delivery centers
        // v1.15 : Force strings to uppercase
        // v1.16 : Force delivery instr to uppercase
        // v1.17 : Change from EnergyConn interface to InfoFactory interface
        // v1.18 : Prepare for G&D region
        //       : - Remove unused regions & logic for them
    	// v1.19 : New server = new paths
    	// v1.20 : Add MA tax jurisdiction
    	// v1.21 : Add PB_DIST delivery center
    	// v1.22 : Finalize support for GAS_REGION
    	// v1.23 : Support for new Valley Oil divisions
    	// v1.24 : Support for B20 Bio Diesel
        System.out.println("ADD Energy -> UPS Location Importer");
        System.out.println("v.1.24 (C) 2006-2014 Patrick Ladd, Bottini Fuel\n");

        if (args.length == 0 || args.length > 2)
        {
            System.out.println("Invalid number of argments:\n" +
                               "Usage:\n" +
                               "   LocationImport [-t] output_dir");
            System.exit(1);
        }
        
        try {
        	if (args[0].compareTo("-t") == 0)
        		new LocationImport(args[1], true);
        	else
            if (args.length == 2 && args[1].compareTo("-t") == 0)
            	new LocationImport(args[0], true);
            else
            	new LocationImport(args[0], false);
        }
        catch (Exception e)
        {
            System.out.println("Uncaught exception:\n" + e);
        }
    }
}
