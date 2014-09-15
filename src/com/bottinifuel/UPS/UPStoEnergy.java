/*
 * Created on Aug 22, 2006 by pladd
 *
 */
package com.bottinifuel.UPS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import au.com.bytecode.opencsv.CSVWriter;

import com.ribomation.fixedwidthfield.Formatter;

/**
 * @author pladd
 *
 */
public class UPStoEnergy
{
    private File InFile;
    private LineNumberReader InStream;

    private File OutFile;

    public UPStoEnergy(String infile, String order_file) throws Exception
    {
        // Open input stream
        InFile = new File(infile);
        try {
            InStream = new LineNumberReader(new FileReader(InFile));
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Error: Input file not found: " + InFile);
            throw e;
        }

        // Open output file
        CSVWriter out;
        OutFile = new File(order_file);
        try {
            out = new CSVWriter(new FileWriter(OutFile), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);

        }
        catch (FileNotFoundException e) {
            System.out.println("Error: Can't create output file: " + OutFile);
            throw e;
        }
            
        // Open input formatter
        Formatter inFmt = new Formatter();
        inFmt.addFwfClass("date", com.ribomation.fixedwidthfield.formatter.DateFWF.class);
        inFmt.setFields(Utils.OrderFileFormatRead);

        int lineNum = 0;
        String output[] = new String[42];
        for (int i = 0; i < output.length; i++)
            output[i] = "";

        try {
            String line;
            while ((line = InStream.readLine()) != null)
            {
                lineNum++;
                Object [] objs = inFmt.parse(line);
                output[Utils.ADD_CSV_CUST_NUM]      = objs[ 0].toString().trim();
                output[Utils.ADD_CSV_TANK_NUM]      = objs[ 2].toString().trim();
                output[Utils.ADD_CSV_ORDER_NUM]     = objs[ 4].toString().trim();
                output[Utils.ADD_CSV_DRIVER_NUM]    = objs[46].toString().trim();
                output[Utils.ADD_CSV_DELIVERY_DATE] = objs[50].toString().trim();
                output[Utils.ADD_CSV_SEQUENCE_NUM]  = objs[42].toString().trim();

                if (objs[26].toString().equals("P")) {
                  output[Utils.ADD_CSV_PHONE_ORD_GAL] = objs[ 6].toString();
                  output[Utils.ADD_CSV_PHONE_ORD_FLAG] = "Y";
                }

                out.writeNext(output);
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
            out.close();
            return;
        }

        out.close();
        
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Utils.ZipToFile(order_file, OutFile.getParent(), "orders-s-" + df.format(new Date()));
        
        System.out.println("Finished exporting UPS orders.\n Processed " + lineNum + " orders.");
    }
    
    /** Print usage message */
    private static void Usage()
    {
        System.out.println("\nUsage: UPSToEnergy {ups_orders} {energy_order_file} ");
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // v1.6  File format change for adding LDD & LDG
        // v1.8  Refactored: constants for field indexes
        // v1.9  Expand comment field to 255
        // v1.10 fix missing phone order units on import
        //        by writing expected drop in phone order units
        // v1.11 : Add new tax jur, del centers
        //       : Messages for invalid jurs, del ctrs, products
        // v1.12 : Save backup copies of exported files
        // v1.13 : Handle left justified numbers in order, cust #, and tank
    	// v1.14 : New server = new paths
        System.out.println("UPSToEnergy v1.14 (c)2006,2007,2008,2009,2010,2011 Patrick Ladd, Bottini Fuel");

        if (args.length != 2)
        {
            System.out.println("EnergyToUPS: Error: Incorrect number of arguments");
            Usage();
            System.exit(1);
        }

        try {
            @SuppressWarnings("unused")
			UPStoEnergy u2e = new UPStoEnergy(args[0], args[1]);
        }
        catch (Exception e)
        {
            System.exit(2);
        }
    }

}
