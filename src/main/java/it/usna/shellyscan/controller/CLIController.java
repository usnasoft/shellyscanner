package it.usna.shellyscan.controller;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import it.usna.shellyscan.model.IPCollection;
import it.usna.shellyscan.model.NonInteractiveDevices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.util.CLI;

public class CLIController {
	private static final String IP_SCAN_PAR_FORMAT = "^((?:(?:0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){2}(?:0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?))\\.(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)-(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
	
	private CLIController() {}
	
	public static void backup(CLI cli, int cliIndex, IPCollection ipCollection, boolean fullScan, final Logger log) {
		final String path = cli.getParameter(cliIndex);
		if(path == null) {
			System.err.println("mandatory parameter after -backup (must be an existing path)");
			System.exit(1);
		}
		Path dirPath = Path.of(path);
		if(Files.exists(dirPath) == false || Files.isDirectory(dirPath) == false) {
			System.err.println("parameter after -backup must be an existing path");
			System.exit(1);
		}
		Predicate<ShellyAbstractDevice> filter = getFilter(cli);
		endCheck(cli);
		log.info("Backup devices in {}", path);
		try (NonInteractiveDevices model = new NonInteractiveDevices(fullScan, ipCollection)) {
			model.execute(d -> {
				try {
					d.backup(Path.of(path, BackupAction.defFileName(d)));
					System.out.println(d.getHostname() + " success");
				} catch (Exception e) {
					System.out.println(d.getHostname() + " error - " + e.toString());
				}
			}, filter);
			log.info("Backup end");
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void restore(CLI cli, int cliIndex, IPCollection ipCollection, boolean fullScan, final Logger log) {
		final String path = cli.getParameter(cliIndex);
		if(path == null) {
			System.err.println("mandatory parameter after -backup (must be an existing path)");
			System.exit(1);
		}
		Path dirPath = Path.of(path);
		if(Files.exists(dirPath) == false || Files.isDirectory(dirPath) == false) {
			System.err.println("parameter after -restore must be an existing path");
			System.exit(1);
		}
		Predicate<ShellyAbstractDevice> filter = getFilter(cli);
		endCheck(cli);
		log.info("Restore devices from {}", path);
		try (NonInteractiveDevices model = new NonInteractiveDevices(fullScan, ipCollection)) {
			model.execute(d -> {
				try {
					String res = RestoreAction.nonInteractiveRestoreDevice(d, dirPath);
					System.out.println(d.getHostname() + (res.isEmpty() ? " success" : (" error - " + res)));
				} catch (FileNotFoundException | NoSuchFileException e1) {
					// just skip
				} catch (Exception e) {
					System.out.println(d.getHostname() + " error - " + e.toString());
				}
			}, filter);
			log.info("Restore end");
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void list(CLI cli, int cliIndex, IPCollection ipCollection, boolean fullScan, final Logger log) {
		String listPar = cli.getParameter(cliIndex);
		boolean ipOnly = false;
		boolean fullList = false;
		if(listPar != null) {
			if(listPar.equals("ip")) {
				ipOnly = true;
			} else if(listPar.equals("full")) {
				fullList = true;
			} else {
				cli.rejectParameter(cliIndex);
			}
		}
		Predicate<ShellyAbstractDevice> filter = getFilter(cli);
		CLIController.endCheck(cli);
		log.info("Retrieving list ...");
		try (NonInteractiveDevices model = new NonInteractiveDevices(fullScan, ipCollection)) {
			if(ipOnly) {
				model.execute(d -> System.out.println(d.getAddressAndPort().toString()), filter);
			} else if(fullList) {
				model.execute(d -> System.out.println(d.getAddressAndPort().toString() + " / " + d), filter);
			} else {
				model.execute(d -> System.out.println(d), filter);
			}
			log.info("List end");
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static IPCollection getIPCollection(CLI cli, int cliIndex) {
		try {
			IPCollection ipCollection = null;
			final Pattern ipRangePattern = Pattern.compile(IP_SCAN_PAR_FORMAT);
			Matcher m = ipRangePattern.matcher(cli.getParameter(cliIndex));
			m.find();
			String baseIPPar = m.group(1);
			int firstIP = Integer.parseInt(m.group(2));
			int lastIP = Integer.parseInt(m.group(3));
			ipCollection = new IPCollection();
			ipCollection.add(baseIPPar, firstIP, lastIP);
			for(int i = 1; (cliIndex = cli.hasEntry("-ipscan" + i, "-ip" + i)) >= 0; i++) {
				m = ipRangePattern.matcher(cli.getParameter(cliIndex));
				m.find();
				baseIPPar = m.group(1);
				firstIP = Integer.parseInt(m.group(2));
				lastIP = Integer.parseInt(m.group(3));
				ipCollection.add(baseIPPar, firstIP, lastIP);
			}
			return ipCollection;
		} catch (Exception e) {
			System.err.println("Wrong parameter format; example: -ipscan 192.168.1.1-254");
			System.exit(1);
			return null ;
		}
	}
	
	private static Predicate<ShellyAbstractDevice> getFilter(CLI cli) {
		int cliIndex;
		if((cliIndex = cli.hasEntry("-gen")) >= 0) {
			String gen = cli.getParameter(cliIndex);
			if(gen == null) {
				System.err.println("Missing value after -gen");
				System.exit(1);
			}
			return  d -> d.getGeneration().equals(gen);
		}
		return null;
	}
	
	// look for unused CLI entries and exit (10) if wrong parameter(s) are detected
	private static void endCheck(CLI cli) {
		if(cli.unused().length > 0) {
			System.err.println("Wrong parameter(s): " + String.join("; ", cli.unused()));
			System.exit(10);
		}
	}
}