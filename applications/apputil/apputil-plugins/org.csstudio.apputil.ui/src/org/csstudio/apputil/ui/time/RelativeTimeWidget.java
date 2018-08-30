/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.ui.time;

import java.util.ArrayList;

import org.csstudio.apputil.time.RelativeTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Widget for displaying and selecting a relative date and time.
 *  <p>
 *  This widget uses Spinner controls to select the time pieces,
 *  and unfortunately those don't allow negative values.
 *  So instead, another checkbox is used to select negative times
 *  'before' some date.
 *  @author Helge Rickens
 *  @author Kay Kasemir
 */
public class RelativeTimeWidget extends Composite
{
    /** Widgets for date pieces. */
    private Text year;
    private Text month, day;
    /** Widgets for time pieces. */
    private Text hour, minute, second;
    /** Widget to select 'before' or 'after' */
    private Button before;

    /** The relative time pieces for year, month, day, hour, minute, second. */
    private RelativeTime relative_time;

    /** Used to prevent recursion when the widget updates the GUI,
     *  which in turn fires listener notifications...
     */
    private boolean in_GUI_update = false;

    final private ArrayList<RelativeTimeWidgetListener> listeners
       = new ArrayList<RelativeTimeWidgetListener>();

    /** Construct widget, initialized to zero offsets.
     *  @param parent Widget parent.
     *  @param flags SWT widget flags.
     */
    public RelativeTimeWidget(Composite parent, int flags)
    {
        this(parent, flags, new RelativeTime());
    }

    /** Construct widget, initialized to given time.
     *  @param parent Widget parent.
     *  @param flags SWT widget flags.
     */
    public RelativeTimeWidget(Composite parent, int flags, RelativeTime relative_time)
    {
        super(parent, flags);
        final GridLayout layout = new GridLayout(4,true);
        setLayout(layout);
        GridData gd;

        // Years: (year)+-  Hours: (hour)+-
        // Month: (month)+- Minutes: (minute)+-
        // Days: (day)+-    Secs: (second)+-
        //
        // [12h] [1 Day] [3 Days] [7 Days]
        // [ ] before?              [now]

        GridData fd = new GridData();
        fd.widthHint = 30;

        // New row (Years / Hours)
        Label l = new Label(this, SWT.NONE);
        l.setText(Messages.Time_Years);
        gd = new GridData(SWT.LEFT, SWT.CENTER, true, true);
        l.setLayoutData(gd);

        year = new Text(this, SWT.BORDER | SWT.WRAP);
        year.setLayoutData(fd);
        year.setToolTipText(Messages.Time_SelectYear);
        year.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
            }
            @Override
            public void focusLost(FocusEvent e) {
                String newText = ((Text)e.widget).getText();
                try {
                    int valueNum = Integer.valueOf(newText);
                    if (valueNum < -19 || valueNum > 10)
                        ((Text)e.widget).setText("");
                }
                catch (NumberFormatException ex) {
                    if (!newText.equals("")) {
                        ((Text)e.widget).setText("");
                    }
                }
            }
        });

        l = new Label(this, SWT.NONE);
        l.setText(Messages.Time_Hours);
        l.setLayoutData(gd);
        hour = new Text(this, SWT.BORDER | SWT.WRAP);
        hour.setToolTipText(Messages.Time_SelectHour);
        hour.setLayoutData(fd);
        //gd = new GridData();
        //gd.grabExcessHorizontalSpace = true;
        //gd.horizontalAlignment = SWT.FILL;
        //hour.setLayoutData(gd);
        //hour.setLayoutData(gd);
        //hour.setMinimum(-23);
        //hour.setMaximum(+23);
        //hour.setIncrement(1);
        //hour.setPageIncrement(6);

        // New row (Month / Minutes)
        l = new Label(this, SWT.NONE);
        l.setText(Messages.Time_Months);
        //gd = new GridData();
        //l.setLayoutData(gd);
        month = new Text(this, SWT.BORDER | SWT.WRAP);
        month.setToolTipText(Messages.Time_SelectMonth);
        month.setLayoutData(fd);
        //gd = new GridData();
        //gd.grabExcessHorizontalSpace = true;
       // gd.horizontalAlignment = SWT.FILL;
        //month.setLayoutData(gd);
        //month.setLayoutData(gd);
        //month.setMinimum(-12);
        //month.setMaximum(+12);
        //month.setIncrement(1);
        //month.setPageIncrement(3);

        l = new Label(this, SWT.NONE);
        l.setText(Messages.Time_Minutes);
        gd = new GridData();
        l.setLayoutData(gd);

        minute = new Text(this, SWT.BORDER | SWT.WRAP);
        minute.setToolTipText(Messages.Time_SelectMinute);
        minute.setLayoutData(fd);
        //gd.grabExcessHorizontalSpace = true;
        //gd.horizontalAlignment = SWT.FILL;
        //minute.setLayoutData(gd);
        //minute.setMinimum(-59);
        //minute.setMaximum(+59);
        //minute.setIncrement(1);
        //minute.setPageIncrement(10);

        // New row (Days / Secs)
        l = new Label(this, SWT.NONE);
        l.setText(Messages.Time_Days);
        gd = new GridData();
        l.setLayoutData(gd);

        day = new Text(this, SWT.BORDER | SWT.WRAP);
        day.setToolTipText(Messages.Time_SelectDay);
        day.setLayoutData(fd);
        gd = new GridData();
        //gd.grabExcessHorizontalSpace = true;
        //gd.horizontalAlignment = SWT.FILL;
        //day.setLayoutData(gd);
        //day.setMinimum(-31);
        //day.setMaximum(+31);
        //day.setIncrement(1);
        //day.setPageIncrement(10);

        l = new Label(this, SWT.NONE);
        l.setText(Messages.Time_Seconds);
        //gd = new GridData();
        //l.setLayoutData(gd);
        //gd.horizontalSpan = 1;
        second = new Text(this, SWT.BORDER | SWT.WRAP);
        second.setToolTipText(Messages.Time_SelectSeconds);
        second.setLayoutData(fd);
        //gd = new GridData();
        //gd.horizontalSpan = 2;
        //gd.grabExcessHorizontalSpace = true;
        //gd.horizontalAlignment = SWT.FILL;
        //second.setLayoutData(gd);
        //second.setMinimum(0);
        //second.setMaximum(59);
        //second.setIncrement(1);
        //second.setPageIncrement(10);

        /*
        // Next Row
        final Label emtpyRow = new Label(this,SWT.NONE);
        emtpyRow.setLayoutData(new GridData(SWT.FILL, SWT.FILL,false, false, layout.numColumns,1));
        emtpyRow.setText(""); //$NON-NLS-1$

        // Next Row
        final Button halfday = new Button(this, SWT.PUSH);
        halfday.setLayoutData(new GridData(SWT.FILL,SWT.FILL,false,false,1,1));
        halfday.setText(Messages.half_day);
        halfday.setToolTipText(Messages.half_day_TT);

        final Button oneday = new Button(this, SWT.PUSH);
        oneday.setLayoutData(new GridData(SWT.FILL,SWT.FILL,false,false,1,1));
        oneday.setText(Messages.one_day);
        oneday.setToolTipText(Messages.one_day_TT);

        final Button threeday = new Button(this, SWT.PUSH);
        threeday.setLayoutData(new GridData(SWT.FILL,SWT.FILL,false,false,1,1));
        threeday.setText(Messages.three_days);
        threeday.setToolTipText(Messages.three_days_TT);

        final Button sevenday = new Button(this, SWT.PUSH);
        sevenday.setLayoutData(new GridData(SWT.FILL,SWT.FILL,false,false,1,1));
        sevenday.setText(Messages.seven_days);
        sevenday.setToolTipText(Messages.seven_days_TT);

        // Next Row for before and now
        before = new Button(this, SWT.CHECK);
        before.setText(Messages.Time_Before);
        before.setToolTipText(Messages.Time_Before_TT);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = layout.numColumns-1;
        gd.horizontalAlignment = SWT.FILL;
        before.setLayoutData(gd);

        Button now = new Button(this, SWT.PUSH);
        now.setText(Messages.Time_Now);
        now.setToolTipText(Messages.Time_Now_TT);
        gd = new GridData(SWT.FILL, SWT.FILL, false, false, 1,1);
        now.setLayoutData(gd);
        */

        // Initialize to given relative time pieces
        setRelativeTime(relative_time);

        /*
        halfday.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (!in_GUI_update)
                {
                    year.setSelection(0);
                    month.setSelection(0);
                    day.setSelection(0);
                    hour.setSelection(12);
                    minute.setSelection(0);
                    second.setSelection(0);
                    updateDataFromGUI();
                }
            }
        });

        oneday.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (!in_GUI_update)
                {
                    year.setSelection(0);
                    month.setSelection(0);
                    day.setSelection(1);
                    hour.setSelection(0);
                    minute.setSelection(0);
                    second.setSelection(0);
                    updateDataFromGUI();
                }
            }
        });

        threeday.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (!in_GUI_update)
                {
                    year.setSelection(0);
                    month.setSelection(0);
                    day.setSelection(3);
                    hour.setSelection(0);
                    minute.setSelection(0);
                    second.setSelection(0);
                    updateDataFromGUI();
                }
            }
        });

        sevenday.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (!in_GUI_update)
                {
                    year.setSelection(0);
                    month.setSelection(0);
                    day.setSelection(7);
                    hour.setSelection(0);
                    minute.setSelection(0);
                    second.setSelection(0);
                    updateDataFromGUI();
                }
            }
        });

        now.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (!in_GUI_update)
                {
                    year.setSelection(0);
                    month.setSelection(0);
                    day.setSelection(0);
                    hour.setSelection(0);
                    minute.setSelection(0);
                    second.setSelection(0);
                    updateDataFromGUI();
                }
            }
        });
        */

        final FocusAdapter update = new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                if (!in_GUI_update)
                    updateDataFromGUI();
            }
        };
        //before.addSelectionListener(update);
        year.addFocusListener(update);
        month.addFocusListener(update);
        day.addFocusListener(update);
        hour.addFocusListener(update);
        minute.addFocusListener(update);
        second.addFocusListener(update);
    }

    /** Add given listener. */
    public void addListener(RelativeTimeWidgetListener listener)
    {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    /** Remove given listener. */
    public void removeListener(RelativeTimeWidgetListener listener)
    {
        listeners.remove(listener);
    }

    /** Set the widget to display the given time.
     *  @see #setNow()
     */
    public void setRelativeTime(RelativeTime relative_time)
    {
        this.relative_time = relative_time;
        updateGUIfromData();
    }

    /** @return Returns the currently selected time. */
    public RelativeTime getRelativeTime()
    {
        return (RelativeTime) relative_time.clone();
    }

    public void externalUpdateDataFromGUI() {
        updateDataFromGUI();
    }

    /** Update the data from the interactive GUI elements. */
    private void updateDataFromGUI()
    {
        //final int sign = before.getSelection() ? -1 : 1;
        final int ymdhms[] = new int[]
        {
            Integer.parseInt(year.getText()),
            Integer.parseInt(month.getText()),
            Integer.parseInt(day.getText()),
            Integer.parseInt(hour.getText()),
            Integer.parseInt(minute.getText()),
            Integer.parseInt(second.getText())
        };
        relative_time = new RelativeTime(ymdhms);
        updateGUIfromData();
    }

    /** Display the current value of the data on the GUI. */
    private void updateGUIfromData()
    {
        in_GUI_update = true;
        final int vals[] = new int[]
        {
            relative_time.get(RelativeTime.YEARS),
            relative_time.get(RelativeTime.MONTHS),
            relative_time.get(RelativeTime.DAYS),
            relative_time.get(RelativeTime.HOURS),
            relative_time.get(RelativeTime.MINUTES),
            relative_time.get(RelativeTime.SECONDS)
        };
        // In principle, the signs could differ "-1years +5days",
        // but in reality that's most often a typo.
        // So check if anything's negative or all is null.
        boolean anything_negative = false;
        boolean all_null = true;
        for (int i=0; i<vals.length; ++i)
        {
            if (vals[i] > 0)
                all_null = false;
            if (vals[i] < 0)
            {
                anything_negative = true;
                all_null = false;
            }
        }
        //final boolean negative = all_null  ||  anything_negative;
        // Apply sign to all
        //before.setSelection(negative);
        //for (int i=0; i<vals.length; ++i)
        //    if (vals[i] < 0)
        //        vals[i] = -vals[i];
        year.setText(String.valueOf(vals[0]));
        month.setText(String.valueOf(vals[1]));
        day.setText(String.valueOf(vals[2]));
        hour.setText(String.valueOf(vals[3]));
        minute.setText(String.valueOf(vals[4]));
        second.setText(String.valueOf(vals[5]));
        in_GUI_update = false;
        // fireUpdatedTimestamp
        for (RelativeTimeWidgetListener l : listeners)
            l.updatedTime(this, relative_time);
    }

}
