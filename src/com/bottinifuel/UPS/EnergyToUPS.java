/*
 * Created on Aug 22, 2006 by pladd
 *
 */
package com.bottinifuel.UPS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import au.com.bytecode.opencsv.CSVReader;

import com.bottinifuel.Energy.JDBC.EnergyConnection;

/**
 * @author pladd
 *
 */
public class EnergyToUPS
{
    private File InFile;
    private FileReader InStream;
    
    private File OutDir;
    private File OilFile;
    private File ProFile;
    private File GasFile;
    
    private FileOutputStream OilStream;
    private FileOutputStream ProStream;
    private FileOutputStream GasStream;

    private boolean ParmsError;
    // *FIXME*
    private String  ParmsFileName = "S:/Delivery/UPS_Logistics/data/LocationImportParms.txt";
//    private String  ParmsFileName = "S:/Delivery/UPS_Logistics/data/TestLocationImportParms.txt";

    private String EnergyHost    = "";
    private int    EnergyPortInt = 0;
    private String EnergyPort    = "";
    private String EnergyDB      = "";
    private String EnergyUser    = "";
    private String EnergyPW      = "";

    private EnergyConnection EnergyConn;

    private void InitParms() throws Exception
    {
        Attributes attrs;
        try {
            Manifest lastUpdate = new Manifest(new FileInputStream(ParmsFileName));
            attrs = lastUpdate.getMainAttributes();
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Error opening last import information: " + e);
            ParmsError = true;
            return;
        }

        String [] varNames = { "EnergyHost", "EnergyPort", "EnergyDB", "EnergyUser", "EnergyPW" }; 
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
            EnergyPortInt = Integer.parseInt(EnergyPort);
        }
        catch (NumberFormatException e)
        {
            System.out.println("Error integer parsing EnergyPort attribute: " + e);
            ParmsError = true;
        }
    }

    
    public EnergyToUPS(String infile, String outDir) throws FileNotFoundException, Exception
    {
/*    public EnergyToUPS(String dslSeason, String infile, String outDir) throws FileNotFoundException, Exception
    {
        if (dslSeason.compareTo("summer") != 0 &&
            dslSeason.compareTo("winter") != 0)
        {
            System.out.println("EnergyToUPS: Error: invalid value for Dyed Diesel season");
            Usage();
            System.exit(2);
        }
*/
        // Open input stream
        InFile = new File(infile);
        try {
            InStream = new FileReader(InFile);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Error: Input file not found: " + InFile);
            throw e;
        }

        // Check output directory
        OutDir = new File(outDir);
        if (!OutDir.isDirectory())
        {
            System.out.println("Error: Output location is not a directory: " + OutDir);
            throw new Exception();
        }

        {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            Utils.ZipToFile(infile, outDir, "orders-r-" + df.format(new Date()));
        }

        // Open output files
        OilFile = new File(OutDir.getAbsolutePath() + File.separator + "orders_oil.txt");
        try { OilStream = new FileOutputStream(OilFile); }
        catch (FileNotFoundException e) {
            System.out.println("Error: Can't create oil output file: " + OilFile);
        }
                
        ProFile = new File(OutDir.getAbsolutePath() + File.separator + "orders_pro.txt");
        try { ProStream = new FileOutputStream(ProFile); }
        catch (FileNotFoundException e) {
            System.out.println("Error: Can't create Propane output file: " + ProFile);
        }
        
        GasFile = new File(OutDir.getAbsolutePath() + File.separator + "orders_gas.txt");
        try { GasStream = new FileOutputStream(GasFile); }
        catch (FileNotFoundException e) {
            System.out.println("Error: Can't create Gas output file: " + GasFile);
        }

        InitParms();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Honor snow drop flag? [n]:");
        String askSnowDrop = in.readLine();
        boolean useSnowDrop = askSnowDrop != null && (askSnowDrop.equals("y") || askSnowDrop.equals("Y"));
        
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
            EnergyConn = new EnergyConnection(EnergyHost, EnergyPortInt, EnergyDB, EnergyUser, EnergyPW);
            s = EnergyConn.getStatement();
        }
        catch (Exception e)
        {
            System.out.println("Error opening Energy Connection: " + e);
            throw e;
        }
        String schedQuery1 = "SELECT period_tank, day_of_week_scheduling, route_table_tank, k_factor, delivery_group, daily_usage_rate " +
        		"FROM dbo.TANKS " +
        		"WHERE account_num = ";
        String schedQuery2 = " AND tank_num = ";
        
        String eqQuery1 = "SELECT serial_num, size_lbs, size_gals " +
                "FROM dbo.EQ " +
                "WHERE account_num = ";
        String eqQuery2 = " AND tank_num = ";

        CSVReader ordersReader = new CSVReader(InStream, ',', (char)0x6);
        int lineNumber = 0;
        int oilOrders = 0;
        int proOrders = 0;
        int gasOrders = 0;

        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        df.setLenient(false);

        Date noDate = df.parse("01/01/2000");

//        Map<Integer, Vector<Order> > DelGroupOrders;
//        DelGroupOrders = new HashMap<Integer, Vector<Order>>();
        Vector<Order> orders = new Vector<Order>();

        boolean fieldCountWarning = false;
        final int expectedFieldCount = 41;

        String [] orderLine;
        while ((orderLine = ordersReader.readNext()) != null) {
            if (orderLine.length == 1)
                continue;
            lineNumber++;
            if (orderLine.length < expectedFieldCount)
            {
                System.out.println("Line " + lineNumber + ": skipped, missing data fields (expected " +
                                   expectedFieldCount + ", got " + orderLine.length + ")");
                continue;
            }
            else if (orderLine.length > expectedFieldCount && !fieldCountWarning)
            {
                System.out.println("Line " + lineNumber + ": extra fields (expected " +
                                   expectedFieldCount + ", got " + orderLine.length + ")");
                fieldCountWarning = true;
            }

            Order order = new Order(Integer.parseInt(orderLine[Utils.ADD_CSV_CUST_NUM]),
                                    Integer.parseInt(orderLine[Utils.ADD_CSV_TANK_NUM]),
                                    Integer.parseInt(orderLine[Utils.ADD_CSV_ORDER_NUM]));
            orders.add(order);
            
            @SuppressWarnings("unused")
			String fill_loc = orderLine[Utils.ADD_CSV_FILL_LOC];
            
            int tankSize     = Integer.parseInt(orderLine[Utils.ADD_CSV_TANK_SIZE]);
            int expectedDrop = Integer.parseInt(orderLine[Utils.ADD_CSV_EXPECTED_DROP]);

            if (expectedDrop > tankSize)
                System.out.println("Warning: Expected drop > Tank Size, acct #" + order.CustNum + " tank #" + order.TankNum);

            int product = Integer.parseInt(orderLine[Utils.ADD_CSV_PRODUCT_CODE]);
            order.setRegion(Utils.ProductToUPSRegion(product));

            order.setPriority(Integer.parseInt(orderLine[Utils.ADD_CSV_PRIORITY]));

            order.setLDD(df.parse(orderLine[Utils.ADD_CSV_LAST_DEL_DATE]));
            order.setLDG((int)Double.parseDouble(orderLine[Utils.ADD_CSV_LAST_DEL_GAL]));

            char dayOfWeek = orderLine[Utils.ADD_CSV_DAY_OF_WEEK_FLAG].charAt(0);

            int shortCustNum = order.CustNum / 10;
            boolean periodTank = false;
            boolean dowTank = false;
            boolean routeTable = false;
            boolean dbdTank = false;
            boolean urdTank = false;
            int dbd = 0;
            @SuppressWarnings("unused")
			double urd = 0.0;
            int del_group = 0;

            ResultSet r = s.executeQuery(schedQuery1 + shortCustNum + schedQuery2 + order.TankNum);
            if (r.next())
            {
                periodTank = r.getString("period_tank").equals("Y");
                dowTank = r.getString("day_of_week_scheduling").equals("Y");
                routeTable = r.getString("route_table_tank").equals("Y");

                del_group = r.getInt("delivery_group");

                if (periodTank && !dowTank && !routeTable)
                {
                    dbd = (int)r.getDouble("k_factor");
                    urd = r.getDouble("daily_usage_rate");
                    if (dbd == 0)
                        urdTank = true;
                    else
                        dbdTank = true;
                }  

                if (r.next())
                    System.out.println("Warning: multiple results found for Acct " + order.CustNum + "/" + order.TankNum);
            }
            else
                System.out.println("Warning: no tank data found for Acct " + order.CustNum + "/" + order.TankNum);
            
            if (( dowTank && dayOfWeek != 'Y') ||
                (!dowTank && dayOfWeek == 'Y'))
                System.out.println("Warning: DOW tank info mismatch: Acct " + order.CustNum + "/" + order.TankNum);

            order.setPhoneOrder(orderLine[Utils.ADD_CSV_PHONE_ORD_FLAG].charAt(0) == 'Y');
            
            int phoneOrderUnits = Integer.parseInt(orderLine[Utils.ADD_CSV_PHONE_ORD_GAL]);

            // Adjust expected drop:
            //  phone order = phone order gallons
            //  day of week = last delivery
            //  propane     = last delivery
            if (order.isPhoneOrder() && phoneOrderUnits > 0)
                expectedDrop = phoneOrderUnits;
            else if (order.getLDG() > 0)
            {
                if (dayOfWeek == 'Y')
                    expectedDrop = order.getLDG();
                else if (product == 6 || product == 7 || product == 8)
                    expectedDrop = order.getLDG();
            }

            @SuppressWarnings("unused")
			String zone = orderLine[Utils.ADD_CSV_ZONE_NUM];
            String zoneTxt = orderLine[Utils.ADD_CSV_ZONE_DESC];

            String snowDropFlag = orderLine[Utils.ADD_CSV_SNOW_DROP];

            switch (product)
            {
            case 2: // #2 oil
                order.setSize1(expectedDrop);
                order.setMax1(tankSize); 
                order.setWeight1((int)(expectedDrop * Utils.OIL_WT_PER_GAL));
                if (order.isPhoneOrder())
                    order.setSelector('P');
                else if (dayOfWeek == 'Y')
                    order.setSelector('W');
                else if (del_group != 0)
                    order.setSelector('R');
                else if (dbdTank || routeTable || urdTank)
                    order.setSelector('Q');
                else if (useSnowDrop && snowDropFlag.equals("Y"))
                    order.setSelector('G');
                break;
            case 6: // propane
            case 7:
            case 8:
                order.setSize1(expectedDrop);
                order.setMax1(tankSize); 
                order.setWeight1((int)(expectedDrop * Utils.PRO_WT_PER_GAL));
                if (order.isPhoneOrder())
                    order.setSelector('P');
                else if (dayOfWeek == 'Y')
                    order.setSelector('W');
                else if (del_group != 0)
                    order.setSelector('R');
                else if (dbdTank || routeTable || urdTank)
                    order.setSelector('Q');
                else if (useSnowDrop && snowDropFlag.equals("Y"))
                    order.setSelector('G');
                break;
            case 9: // kero
                order.setSize2(expectedDrop);
                order.setMax2(tankSize); 
                order.setWeight2((int)(expectedDrop * Utils.KER_WT_PER_GAL));
                if (order.isPhoneOrder())
                    order.setSelector('J');
                else if (dayOfWeek == 'Y')
                    order.setSelector('Z');
                else if (del_group != 0)
                    order.setSelector('T');
                else if (dbdTank || routeTable || urdTank)
                    order.setSelector('L');
                else if (useSnowDrop && snowDropFlag.equals("Y"))
                    order.setSelector('H');
                else
                    order.setSelector('K');
                break;
            case 10: // clear kero
                order.setSize2(expectedDrop);
                order.setMax2(tankSize); 
                order.setWeight2((int)(expectedDrop * Utils.DSL_WT_PER_GAL));
                if (order.isPhoneOrder())
                    order.setSelector('J');
                else if (dayOfWeek == 'Y')
                    throw new Exception("Can't handle day of week clear kero account #" + order.CustNum + '/' + order.TankNum);
                else if (del_group != 0)
                    throw new Exception("Can't handle delivery group clear kero account #" + order.CustNum + '/' + order.TankNum);
                else if (dbdTank || routeTable || urdTank)
                    throw new Exception("Can't handle period delivery clear kero account #" + order.CustNum + '/' + order.TankNum);
                else if (useSnowDrop && snowDropFlag.equals("Y"))
                    throw new Exception("Can't handle snow drop clear kero account #" + order.CustNum + '/' + order.TankNum);
                else
                    order.setSelector('K');
                break;
            case 11: // winter blend
                order.setSize3(expectedDrop);
                order.setMax3(tankSize); 
                order.setWeight3((int)(expectedDrop * Utils.OIL_WT_PER_GAL));
                if (order.isPhoneOrder())
                    order.setSelector('A');
                else if (dayOfWeek == 'Y')
                    order.setSelector('Y');
                else if (del_group != 0)
                    order.setSelector('U');
                else if (dbdTank || routeTable || urdTank)
                    order.setSelector('N');
                else if (useSnowDrop && snowDropFlag.equals("Y"))
                    order.setSelector('M');
                else
                    order.setSelector('B');
                break;
            case 15:
            case 25:
                order.setSize1(expectedDrop);
                order.setMax1(tankSize); 
                order.setWeight1((int)(expectedDrop * Utils.GAS_WT_PER_GAL));
                if (order.isPhoneOrder())
                    order.setSelector('C');
                else if (dayOfWeek == 'Y')
                    order.setSelector('B');
                else if (del_group != 0)
                    order.setSelector('F');
                else if (dbdTank || routeTable || urdTank)
                    order.setSelector('D');
                else if (useSnowDrop && snowDropFlag.equals("Y"))
                    order.setSelector('S');
                else
                	order.setSelector('A');
                break;
            case 16:
            case 26:
                order.setSize2(expectedDrop);
                order.setMax2(tankSize); 
                order.setWeight2((int)(expectedDrop * Utils.GAS_WT_PER_GAL));
                if (order.isPhoneOrder())
                    order.setSelector('M');
                else if (dayOfWeek == 'Y')
                    order.setSelector('N');
                else if (del_group != 0)
                    order.setSelector('G');
                else if (dbdTank || routeTable || urdTank)
                    order.setSelector('O');
                else if (useSnowDrop && snowDropFlag.equals("Y"))
                    order.setSelector('T');
                else
                	order.setSelector('L');
                break;
            case 18:
            case 28:
                order.setSize3(expectedDrop);
                order.setMax3(tankSize); 
                order.setWeight3((int)(expectedDrop * Utils.GAS_WT_PER_GAL));
                if (order.isPhoneOrder())
                    order.setSelector('X');
                else if (dayOfWeek == 'Y')
                    order.setSelector('U');
                else if (del_group != 0)
                    order.setSelector('H');
                else if (dbdTank || routeTable || urdTank)
                    order.setSelector('V');
                else if (useSnowDrop && snowDropFlag.equals("Y"))
                    order.setSelector('Y');
                else
                	order.setSelector('Z');
                break;
            case 19: // Clear DSL
            case 21: // Clear DSL no tax
                order.setSize1(expectedDrop);
                order.setMax1(tankSize); 
                order.setWeight1((int)(expectedDrop * Utils.DSL_WT_PER_GAL));
                if (order.isPhoneOrder())
                    order.setSelector('P');
                else if (dayOfWeek == 'Y')
                    order.setSelector('W');
                else if (del_group != 0)
                    order.setSelector('E');
                else if (dbdTank || routeTable || urdTank)
                    order.setSelector('Q');
                else if (useSnowDrop && snowDropFlag.equals("Y"))
                    order.setSelector('R');
                break;
            case 20: // Dyed DSL
//                if (dslSeason.compareTo("winter") == 0)
//                {
//                    region = Utils.BLEND_REGION;
//                    size2(expectedDrop;
//                    max2(tankSize; 
//                    weight2 = (int)(expectedDrop * DSL_WT_PER_GAL;
//                }
//                else
//                {
//                    order.setRegion(Utils.OIL_REGION);
                    order.setSize1(expectedDrop);
                    order.setMax1(tankSize); 
                    order.setWeight1((int)(expectedDrop * Utils.DSL_WT_PER_GAL));
                    if (order.isPhoneOrder())
                        order.setSelector('C');
                    else if (dayOfWeek == 'Y')
                        order.setSelector('X');
                    else if (del_group != 0)
                        order.setSelector('S');
                    else if (dbdTank || routeTable || urdTank)
                        order.setSelector('F');
                    else
                        order.setSelector('D');
//                }
                break;
            case 22: // B20 Bio Diesel
                order.setSize1(expectedDrop);
                order.setMax1(tankSize); 
                order.setWeight1((int)(expectedDrop * Utils.DSL_WT_PER_GAL));
                order.setSelector('I');
                break;
            	
            default:
                System.out.println("Warning: Unknown product code \"" + product + "\", line #" +
                                   lineNumber);
                break;
            }
            
//            if (zone.equals("763") ||
//                zone.equals("764"))
            if (zoneTxt.equals("E") ||
                zoneTxt.equals("U") ||
                zoneTxt.equals("O"))
                    order.setSelector('E');

            if (zoneTxt.equals("VAL"))
            {
            	if (product == 9)
                	order.setSelector('I');
            	else if (product == 2)
            	{
            		if (order.isPhoneOrder())
            			order.setSelector('V');
            		else
            			order.setSelector('O');
            	}
            	else
            		throw new Exception("Invalid Valley Oil product acct " + order.CustNum + '/' + order.TankNum);
            }

            if (dbdTank)
            {
                if ((((product == 6 || product == 7 || product == 8) && dbd <= 28) ||
                    (product != 6 && product != 7 && product != 8)) &&
                    order.getPriority() != 100)
                {
                    System.out.println("Warning: Forcing DBD priority to 100 from " +
                                       order.getPriority() + ", Acct " + order.CustNum + "/" + order.TankNum);
                    order.setPriority(100);
                }
            }
            if (routeTable && order.getPriority() != 100)
            {
                System.out.println("Warning: Forcing RTB priority to 100 from " +
                                   order.getPriority() + ", Acct " + order.CustNum + "/" + order.TankNum);
                order.setPriority(100);
            }
            if (order.isPhoneOrder() && order.getPriority() != 100)
            {
                System.out.println("Warning: Forcing phone order priority to 100 from " +
                                   order.getPriority() + ", Acct " + order.CustNum + "/" + order.TankNum);
                order.setPriority(100);
            }
            if (dayOfWeek == 'Y' && order.getPriority() != 100)
            {
                System.out.println("Warning: Forcing day of week priority to 100 from " +
                                   order.getPriority() + ", Acct " + order.CustNum + "/" + order.TankNum);
                order.setPriority(100);
            }

            order.setSchedDate(df.parse(orderLine[Utils.ADD_CSV_SCHED_DATE]));
            order.setRunoutDate(df.parse(orderLine[Utils.ADD_CSV_RUNOUT_DATE]));

            @SuppressWarnings("unused")
			Date phoneOrderDate;
            if (order.isPhoneOrder() && !orderLine[Utils.ADD_CSV_PHONE_ORD_DATE].equals(""))
            {
                phoneOrderDate = df.parse(orderLine[Utils.ADD_CSV_PHONE_ORD_DATE]);
            }

            if (order.getRunoutDate().before(order.getSchedDate()))
                order.setRunoutDate(order.getSchedDate());

            if (orderLine[Utils.ADD_CSV_COMMENT].trim().length() > 0)
                order.setComment(orderLine[Utils.ADD_CSV_COMMENT].trim() + " // ");

            // propane - show any equipment on comment line
            if (product == 6 || product == 7 || product == 8)
            {
                String eq = "";
                ResultSet eq_r = s.executeQuery(eqQuery1 + shortCustNum + eqQuery2 + order.TankNum);
                while (eq_r.next())
                {
                    eq += eq_r.getString("serial_num").trim() + "(";
                    int gals = eq_r.getInt("size_gals");
                    if (gals == 0)
                        eq += "_______";
                    else
                        eq += gals;
                    eq += ")";
                }
                if (eq.length() == 0)
                    order.setComment(order.getComment() + "EQ: _____________________________ ");
                else
                    order.setComment(order.getComment() + "EQ: " + eq + " ");
            }

            if (order.getLDG() == 0 && order.getLDD().equals(noDate))
                order.setComment(order.getComment() + "LDD: xxx LDG: xxx");
            else
                order.setComment(order.getComment() + "LDD:" + df.format(order.getLDD()) + " LDG:" + order.getLDG());
        }
        ordersReader.close();

        for (Order order : orders)
        {
            String out = order.OrderLine();

            switch (order.getRegion())
            {
            case Utils.OIL_REGION:
                oilOrders++;
                OilStream.write(out.getBytes());
                OilStream.write('\n');
                break;
            case Utils.PROPANE_REGION:
                proOrders++;
                ProStream.write(out.getBytes());
                ProStream.write('\n');
                break;
            case Utils.GAS_REGION:
                gasOrders++;
                GasStream.write(out.getBytes());
                GasStream.write('\n');
                break;
            }
        }

        System.out.println("Orders Processed:" +
                           "\n Total:   " + lineNumber +
                           "\n Oil:     " + oilOrders +
                           "\n Propane: " + proOrders + 
                           "\n Gas:     " + gasOrders);
        if (lineNumber != (oilOrders + proOrders + gasOrders))
            System.out.println("Error: Mismatch in total order count!");
    }

    

    /** Print usage message */
    private static void Usage()
    {
        System.out.println("\nUsage: EnergyToUPS {order_file} {output_dir}");
    }

    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // v1.5  : Make runout date = sched date if earlier - prevents error message on UPS import
        // v1.6  : Added LDD & LDG output
        // v1.7  : Put LDD & LDG in order comment
        // v1.8  : Refactored: constants for field indexes
        //       : xxx for invalid LDD & LDG
        // v1.9  : Modified selectors
        //       : Commented out winter/summer handling
        //       : Moved winter blend & dyed diesel to Oil Region
        // v1.10 : All propane uses LDG for Expected gallons, except phone order
        // v1.11 : Prompt - period tickets?
        // v1.12 : Don't put selectors on period tickets
        // v1.13 : Read tank parms out of energy db
        // v1.14 : Dyed diesel period selector = F
        // v1.15 : Expected drop > tank size warning
        // v1.16 : Related accounts handling
        //       : Propane DBD > 28 keep priority as calculated
        // v1.17 : Handle URD accounts
        // v1.18 : Phone Orders & DOW trump delivery group
        // v1.19 : Set Expected = LDG only if LDG != 0
        // v1.20 : Up length of special instructions to 256
        // v1.21 : Add new tax jur, del centers
        //       : Messages for invalid jurs, del ctrs, products
        // v1.22 : Add Propane equipment serial numbers
        // v1.23 : Move special instructions to front of comment
        // v1.24 : Add new economy zone - change to look at zoneTxt
        // v1.25 : Force text fields to uppercase
        // v1.26 : Add snow drop flag handling
        // v1.27 : Add new tax jurisdictions
        // v1.28 : Save backup copies of imported files
        // v1.29 : Handle left justified order, cust #, and tank on output
        // v1.30 : Prepare for G&D region
        //       : - Remove unused regions & logic for them
    	// v1.31 : New server = new paths
    	// v1.32 : Finish GAS_REGION code
    	//       : Remove summer|winter flag
    	// v1.33 : Handle upgrade to ADD v10.01.09-HF5 - special characters no longer filtered from output
    	// v1.34 : Valley Oil 1st Pass - add del center 23 "VALLEY" and V selector
    	// v1.35 : Final Valley oil setup - add O & I selectors, remove DyedD I selector
    	// v1.36 : Remove test import parms file
    	// v1.37 : Support B20 Bio Diesel
        System.out.println("EnergyToUPS v1.37 (c)2006-2014 Patrick Ladd, Bottini Fuel");
        if (args.length != 2)
        {
            System.out.println("EnergyToUPS: Error: Incorrect number of arguments");
            Usage();
            System.exit(1);
        }

        try {
			new EnergyToUPS(args[0], args[1]);
        }
        catch (Exception e)
        {
            System.out.println("Uncaught exception: " + e);
            System.exit(2);
        }
    }
}
