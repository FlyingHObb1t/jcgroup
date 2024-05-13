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
import java.nio.ByteBuffer;
import java.util.List;

public class MemoryLimitsTest {

  private static final Logger LOG = LoggerFactory.getLogger(MemoryLimitsTest.class);
  private static Admin admin;
  private static Group root;
  private static Group one;
  private static Group two;

  @BeforeClass
  public static void setUpClass() {
    try {
      int subsystems = Constants.SUBSYS_MEMORY;
      admin = new Admin(subsystems);
      root = admin.getRootGroup();
      long createGroupStartTime = System.currentTimeMillis();
      one = admin.createGroup("one", subsystems);
      System.out.println("create " + Shell.getSubsystemsFlag(subsystems).toString() + " cgroup takes " + (System.currentTimeMillis() - createGroupStartTime) + " ms");
      two = admin.createGroup("two", subsystems);
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
//    try {
//      admin.umount();
//    } catch (IOException e) {
//      LOG.error("Umount cgroup failed.", e);
//      assertTrue(false);
//    }
  }

  @Test
  public void testMemoryLimit() {
    long memoryLimitSmall = 1024 * 1024 * 2;
    long memoryLimitBig = 1024 * 1024 * 32;
    try {
      long setCpusetStartTime = System.currentTimeMillis();
      one.getMemory().setPhysicalUsageLimit(memoryLimitSmall);
      System.out.println("set memory takes " + (System.currentTimeMillis() - setCpusetStartTime) + " ms");
      two.getMemory().setPhysicalUsageLimit(memoryLimitBig);
      final Group oneTmp = one;
      final Group twoTmp = two;
      final int memoryAllocationSize =  1024 * 1024 * 256;
      new Thread(){
        @Override
        public void run() {
          int id = Threads.getThreadId();
          LOG.info("Thread id:" + id);
          try {
            long addTaskToMemorySubsystemStartTime = System.currentTimeMillis();
            oneTmp.getMemory().addTask(id);
            System.out.println("add task to memory subsystem takes " + (System.currentTimeMillis() - addTaskToMemorySubsystemStartTime) + " ms");
            ByteBuffer byteBuffer = ByteBuffer.allocate(memoryAllocationSize);
            System.out.println("Cgroup 1 byteBuffer capacity=" + byteBuffer.capacity());
            // Fill the buffer with the byte value 0x01
            for (int i = 0; i < byteBuffer.capacity(); i++) {
              byteBuffer.put((byte) 0x01);
            }
            System.out.println("Cgroup 1 memory usage=" + oneTmp.getMemory().getPhysicalUsage());
            while(true);
          } catch (IOException e) {
            LOG.error("Test memory failed.", e);
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
            twoTmp.getMemory().addTask(id);
            ByteBuffer byteBuffer = ByteBuffer.allocate(memoryAllocationSize);
            System.out.println("Cgroup 2 byteBuffer capacity=" + byteBuffer.capacity());
            for (int i = 0; i < byteBuffer.capacity(); i++) {
              byteBuffer.put((byte) 0x01);
            }
            System.out.println("Cgroup 2 memory usage=" + twoTmp.getMemory().getPhysicalUsage());
            while (true);
          } catch (IOException e) {
            LOG.error("Test memory failed.", e);
            assertTrue(false);
          }
        }
      }.start();
      Thread.sleep(6000000l);
    } catch (Exception e) {
      LOG.error("Test memory failed.", e);
      e.printStackTrace();
      assertTrue(false);
    }
  }
}
