package me.haosdent.cgroup.threads;

import me.haosdent.cgroup.manage.Admin;
import me.haosdent.cgroup.manage.Group;
import me.haosdent.cgroup.util.Constants;
import me.haosdent.cgroup.util.Shell;
import me.haosdent.cgroup.util.Threads;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

public class CpuSharesTest {

  private static final Logger LOG = LoggerFactory.getLogger(CpuSharesTest.class);
  private static Admin admin;
  private static Group root;
  private static Group one;
  private static Group two;

  @BeforeClass
  public static void setUpClass() {
    try {
      admin = new Admin(Constants.SUBSYS_CPUSET | Constants.SUBSYS_CPU);
      root = admin.getRootGroup();
      long createGroupStartTime = System.currentTimeMillis();
      one = admin.createGroup("one", Constants.SUBSYS_CPUSET | Constants.SUBSYS_CPU);
      System.out.println("create " + Shell.getSubsystemsFlag(Constants.SUBSYS_CPUSET | Constants.SUBSYS_CPU).toString() + " cgroup takes " + (System.currentTimeMillis() - createGroupStartTime) + " ms");
      two = admin.createGroup("two", Constants.SUBSYS_CPUSET | Constants.SUBSYS_CPU);
      System.out.println("Added two groups, print all current groups");
      List<Group> cgroups = admin.getGroupList();
      for (Group cgroup : cgroups) {
        System.out.println(cgroup.getName());
      }
    } catch (IOException e) {
      LOG.error("Create cgroup Failed.", e);
      assertTrue(false);
    }
  }

  @AfterClass
  public static void tearDownClass() {
    try {
      admin.umount();
    } catch (IOException e) {
      LOG.error("Umount cgroup failed.", e);
      assertTrue(false);
    }
  }

  @Test
  public void testCpu() {
    try {
      long setCpusetStartTime = System.currentTimeMillis();
      one.getCpuset().setCpus(new int[]{0});
      System.out.println("set cpuset takes " + (System.currentTimeMillis() - setCpusetStartTime) + " ms");
      two.getCpuset().setCpus(new int[]{0});
      long setMemNodeStartTime = System.currentTimeMillis();
      one.getCpuset().setMems(new int[]{0});
      System.out.println("set memory node takes " + (System.currentTimeMillis() - setMemNodeStartTime) + " ms");
      two.getCpuset().setMems(new int[]{0});
      long setCpuShareStartTime = System.currentTimeMillis();
      one.getCpu().setShares(512);
      System.out.println("set cpu shares takes " + (System.currentTimeMillis() - setCpuShareStartTime) + " ms");
      two.getCpu().setShares(2048);
      final Group oneTmp = one;
      final Group twoTmp = two;
      new Thread(){
        @Override
        public void run() {
          int id = Threads.getThreadId();
          LOG.info("Thread id:" + id);
          try {
            long addTaskToCpuSubsystemStartTime = System.currentTimeMillis();
            oneTmp.getCpu().addTask(id);
            long addTaskToCpuSubsystemEndTime = System.currentTimeMillis();
            oneTmp.getCpuset().addTask(id);
            System.out.println("add task to cpuset subsystem takes " + (System.currentTimeMillis() - addTaskToCpuSubsystemEndTime) + " ms");
            System.out.println("add task to cpu subsystem takes " + (addTaskToCpuSubsystemEndTime - addTaskToCpuSubsystemStartTime) + " ms");
            while (true);
          } catch (IOException e) {
            LOG.error("Test cpu failed.", e);
            assertTrue(false);
          }
        }
      }.start();
      new Thread(){
        @Override
        public void run() {
          int id = Threads.getThreadId();
          LOG.info("Thread id:" + id);
          try {
            twoTmp.getCpu().addTask(id);
            twoTmp.getCpuset().addTask(id);
            while (true);
          } catch (IOException e) {
            LOG.error("Test cpu failed.", e);
            assertTrue(false);
          }
        }
      }.start();
      Thread.sleep(6000000l);
    } catch (Exception e) {
      LOG.error("Test cpu failed.", e);
      assertTrue(false);
    }
  }
}
