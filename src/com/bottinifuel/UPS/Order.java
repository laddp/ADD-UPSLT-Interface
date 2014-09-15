/*
 * Created on Nov 14, 2007 by pladd
 *
 */
package com.bottinifuel.UPS;

import java.util.Date;

import com.ribomation.fixedwidthfield.Formatter;

/**
 * @author pladd
 *
 */
public class Order
{
    public final int CustNum;
    public final int TankNum;
    public final int OrderNum;
    
    private int Region = 0;

    private int Size1 = 0;
    private int Size2 = 0;
    private int Size3 = 0;
    
    private int Max1 = 0;
    private int Max2 = 0;
    private int Max3 = 0;

    private int Weight1 = 0;
    private int Weight2 = 0;
    private int Weight3 = 0;

    private boolean PhoneOrder;
    private char    Selector = ' ';
    
    private int Priority;
    
    private String Comment;
    
    private Date SchedDate;
    private Date RunoutDate;
    
    private Date LDD;
    private int  LDG;

    public Order(int cust, int tank, int order)
    {
        CustNum  = cust;
        TankNum  = tank;
        OrderNum = order;
    }
    
    public String OrderLine()
    {
        char phone = PhoneOrder ? 'Y' : 'N';
        Object [] outLine = { CustNum,    null, TankNum,       null, OrderNum, null,
                              Size1,      null, Size2,         null, Size3,    null,
                              Max1,       null, Max2,          null, Max3,     null,
                              Weight1,    null, Weight2,       null, Weight3,  null,
                              phone,      null, Selector,      null,
                              SchedDate,  null, RunoutDate,    null, Priority, null,
                              Comment,    null, LDD,           null, LDG,      null,
                              "",         null, "",            null, "",       null,
                              "",         null, "",            null, "" }; 

        Formatter outFmt = new Formatter();
        outFmt.addFwfClass("date", com.ribomation.fixedwidthfield.formatter.DateFWF.class);
        outFmt.setFields(Utils.OrderFileFormatWrite);

        return outFmt.format(outLine);
    }

    public int getRegion()
    {
        return Region;
    }

    public void setRegion(int region)
    {
        Region = region;
    }

    public int getSize1()
    {
        return Size1;
    }

    public void setSize1(int size1)
    {
        Size1 = size1;
    }

    public int getSize2()
    {
        return Size2;
    }

    public void setSize2(int size2)
    {
        Size2 = size2;
    }

    public int getSize3()
    {
        return Size3;
    }

    public void setSize3(int size3)
    {
        Size3 = size3;
    }

    public int getMax1()
    {
        return Max1;
    }

    public void setMax1(int max1)
    {
        Max1 = max1;
    }

    public int getMax2()
    {
        return Max2;
    }

    public void setMax2(int max2)
    {
        Max2 = max2;
    }

    public int getMax3()
    {
        return Max3;
    }

    public void setMax3(int max3)
    {
        Max3 = max3;
    }

    public int getWeight1()
    {
        return Weight1;
    }

    public void setWeight1(int weight1)
    {
        Weight1 = weight1;
    }

    public int getWeight2()
    {
        return Weight2;
    }

    public void setWeight2(int weight2)
    {
        Weight2 = weight2;
    }

    public int getWeight3()
    {
        return Weight3;
    }

    public void setWeight3(int weight3)
    {
        Weight3 = weight3;
    }

    public boolean isPhoneOrder()
    {
        return PhoneOrder;
    }

    public void setPhoneOrder(boolean phoneOrder)
    {
        PhoneOrder = phoneOrder;
    }

    public char getSelector()
    {
        return Selector;
    }

    public void setSelector(char selector)
    {
        Selector = selector;
    }

    public int getPriority()
    {
        return Priority;
    }

    public void setPriority(int priority)
    {
        Priority = priority;
    }

    public String getComment()
    {
        if (Comment == null)
            return "";
        else
            return Comment;
    }

    public void setComment(String comment)
    {
        Comment = comment.toUpperCase();
    }

    public Date getSchedDate()
    {
        return SchedDate;
    }

    public void setSchedDate(Date schedDate)
    {
        SchedDate = schedDate;
    }

    public Date getRunoutDate()
    {
        return RunoutDate;
    }

    public void setRunoutDate(Date runoutDate)
    {
        RunoutDate = runoutDate;
    }

    public Date getLDD()
    {
        return LDD;
    }

    public void setLDD(Date ldd)
    {
        LDD = ldd;
    }

    public int getLDG()
    {
        return LDG;
    }

    public void setLDG(int ldg)
    {
        LDG = ldg;
    }
}
