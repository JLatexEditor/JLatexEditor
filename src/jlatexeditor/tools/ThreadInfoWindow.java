package jlatexeditor.tools;

import javax.swing.*;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

public class ThreadInfoWindow extends JFrame implements Runnable {
  private JTextArea info = new JTextArea();
  private JLabel working;
  private JLabel message = new JLabel("<html>If you experience problems, please send include this stack trace in your bug report.</html>");

  public ThreadInfoWindow() {
    super("Stack Trace");
    setBackground(Color.WHITE);

    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());
    cp.setBackground(Color.WHITE);

    JScrollPane scrollPane = new JScrollPane(info);
    cp.add(scrollPane);
    scrollPane.setPreferredSize(new Dimension(600,800));

    working = new JLabel(new ImageIcon(getClass().getResource("/images/working32.gif")));
    working.setText("collecting CPU usage information...");
    cp.add(working, BorderLayout.NORTH);

    message.setHorizontalAlignment(JLabel.CENTER);
    message.setHorizontalTextPosition(JLabel.CENTER);
    message.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    cp.add(message, BorderLayout.SOUTH);
    
    setVisible(true);
    pack();

    new Thread(this).start();
  }

  public void run() {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
    long[] ids = bean.getAllThreadIds();
    ThreadInfo[] threadInfos = bean.getThreadInfo(ids, 100);

    // collect CPU times
    ThreadCPUInfo[] threadCPUInfos = new ThreadCPUInfo[ids.length];
    if(bean.isThreadCpuTimeSupported()) {
      long[] initTime = new long[ids.length];
      for(int nr = 0; nr < ids.length; nr++) initTime[nr] = bean.getThreadCpuTime(ids[nr]);

      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) { }

      for(int nr = 0; nr < ids.length; nr++) {
        threadCPUInfos[nr] = new ThreadCPUInfo(threadInfos[nr], bean.getThreadCpuTime(ids[nr]) - initTime[nr]);
      }
    }

    Arrays.sort(threadCPUInfos);

    StringBuffer builder = new StringBuffer();
    for(int nr = 0; nr < ids.length; nr++) {
      ThreadCPUInfo threadCPUInfo = threadCPUInfos[nr];
      ThreadInfo info = threadCPUInfo.getThreadInfo();
      if(info == null) continue;
      long cpuTime = threadCPUInfo.getCpuTime();

      builder.append("Thread name: " + info.getThreadName() + "\n");
      builder.append("CPU time: " + (cpuTime >= 0 ? cpuTime/ 2000000000. : "thread has died already") + "\n");

      builder.append("Stack trace:\n");
      StackTraceElement[] trace = info.getStackTrace();
      for(StackTraceElement traceElement : trace) builder.append("\tat " + traceElement).append("\n");
      builder.append("\n");
    }

    info.setText(builder.toString());
    working.setVisible(false);
  }

  private static class ThreadCPUInfo implements Comparable<ThreadCPUInfo> {
    private ThreadInfo threadInfo;
    private long cpuTime;

    private ThreadCPUInfo(ThreadInfo threadInfo, long cpuTime) {
      this.cpuTime = cpuTime;
      this.threadInfo = threadInfo;
    }

    public ThreadInfo getThreadInfo() {
      return threadInfo;
    }

    public long getCpuTime() {
      return cpuTime;
    }

    public int compareTo(ThreadCPUInfo o) {
      return new Long(o.cpuTime).compareTo(cpuTime);
    }
  }
}
