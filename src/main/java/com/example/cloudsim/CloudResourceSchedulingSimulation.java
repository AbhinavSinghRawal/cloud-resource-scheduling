package com.example.cloudsim;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CloudResourceSchedulingSimulation {

    private static final int HOSTS = 2;
    private static final int HOST_PES = 4;
    private static final int VMS = 4;
    private static final int VM_PES = 2;
    private static final int TASKS = 12;

    private static final long HOST_MIPS = 10000;
    private static final long VM_MIPS = 2000;
    private static final long CLOUDLET_MI = 20000;

    public static void main(String[] args) throws Exception {

        String mode = args.length > 0
                ? args[0].toLowerCase(Locale.ROOT)
                : "timeshared";

        if (!mode.equals("timeshared")
                && !mode.equals("spaceshared")) {

            System.out.println(
                    "Use argument timeshared or spaceshared."
            );
            return;
        }

        // Create CloudSim simulation
        CloudSimPlus sim = new CloudSimPlus();

        // Create hosts
        List<Host> hosts = new ArrayList<>();

        for (int h = 0; h < HOSTS; h++) {

            List<Pe> pes = new ArrayList<>();

            for (int p = 0; p < HOST_PES; p++) {
                pes.add(new PeSimple(HOST_MIPS));
            }

            Host host = new HostSimple(
                    16384,
                    100000,
                    1000000,
                    pes
            );

            hosts.add(host);
        }

        // Create datacenter
        new DatacenterSimple(sim, hosts);

        // Create broker
        DatacenterBroker broker =
                new DatacenterBrokerSimple(sim);

        // Create virtual machines
        List<Vm> vms = new ArrayList<>();

        for (int i = 0; i < VMS; i++) {

            Vm vm = new VmSimple(
                    i,
                    VM_MIPS,
                    VM_PES
            );

            vm.setRam(2048)
              .setBw(10000)
              .setSize(20000);

            if (mode.equals("spaceshared")) {
                vm.setCloudletScheduler(
                        new CloudletSchedulerSpaceShared()
                );
            } else {
                vm.setCloudletScheduler(
                        new CloudletSchedulerTimeShared()
                );
            }

            vms.add(vm);
        }

        // Create cloudlets (tasks)
        List<Cloudlet> tasks = new ArrayList<>();

        for (int i = 0; i < TASKS; i++) {

            long length =
                    CLOUDLET_MI + (i % 4) * 5000L;

            Cloudlet cloudlet = new CloudletSimple(
                    i,
                    length,
                    1
            );

            cloudlet.setFileSize(300);
            cloudlet.setOutputSize(300);

            cloudlet.setUtilizationModel(
                    new UtilizationModelFull()
            );

            // Assign tasks to VMs in round-robin order
            cloudlet.setVm(vms.get(i % VMS));

            tasks.add(cloudlet);
        }

        // Submit VMs and cloudlets
        broker.submitVmList(vms);
        broker.submitCloudletList(tasks);

        System.out.println();
        System.out.println(
                "CloudSim Plus - Scheduler: " + mode
        );

        System.out.println(
                "Hosts: " + HOSTS
                + " | VMs: " + VMS
                + " | Tasks: " + TASKS
        );

        // Run simulation
        sim.start();

        // Retrieve finished tasks
        List<Cloudlet> finished =
                new ArrayList<>(
                        broker.getCloudletFinishedList()
                );

        finished.sort(
                Comparator.comparingLong(Cloudlet::getId)
        );

        // Display table header
        System.out.printf(
                "%-8s %-6s %-12s %-12s %-12s %-10s%n",
                "Task",
                "VM",
                "Status",
                "Start(s)",
                "Finish(s)",
                "CPU(s)"
        );

        double finishSum = 0;
        double cpuSum = 0;
        double makespan = 0;

        // Save results to CSV
        String filename = "results-" + mode + ".csv";

        try (PrintWriter out = new PrintWriter(
                new FileWriter(filename))) {

            out.println(
                    "scheduler,task_id,vm_id,status,"
                    + "start_s,finish_s,cpu_s,length_mi"
            );

            for (Cloudlet cloudlet : finished) {

                double startTime =
                        cloudlet.getStartTime();

                double finishTime =
                        cloudlet.getFinishTime();

                double cpuTime =
                        finishTime - startTime;

                System.out.printf(
                        "%-8d %-6d %-12s %-12.2f "
                        + "%-12.2f %-10.2f%n",
                        cloudlet.getId(),
                        cloudlet.getVm().getId(),
                        cloudlet.getStatus(),
                        startTime,
                        finishTime,
                        cpuTime
                );

                finishSum += finishTime;
                cpuSum += cpuTime;

                makespan = Math.max(
                        makespan,
                        finishTime
                );

                out.printf(
                        Locale.US,
                        "%s,%d,%d,%s,%.4f,%.4f,%.4f,%d%n",
                        mode,
                        cloudlet.getId(),
                        cloudlet.getVm().getId(),
                        cloudlet.getStatus(),
                        startTime,
                        finishTime,
                        cpuTime,
                        cloudlet.getLength()
                );
            }
        }

        // Display simulation summary
        System.out.println();

        System.out.println(
                "Submitted cloudlets: " + tasks.size()
        );

        System.out.println(
                "Finished cloudlets: " + finished.size()
        );

        System.out.println(
                "Unfinished cloudlets: "
                + (tasks.size() - finished.size())
        );

        System.out.printf(
                Locale.US,
                "Average finish time: %.2f s%n",
                finished.isEmpty()
                        ? 0.0
                        : finishSum / finished.size()
        );

        System.out.printf(
                Locale.US,
                "Makespan: %.2f s%n",
                makespan
        );

        System.out.printf(
                Locale.US,
                "Total CPU time: %.2f s%n",
                cpuSum
        );

        System.out.println(
                "CSV saved: " + filename
        );
    }
}