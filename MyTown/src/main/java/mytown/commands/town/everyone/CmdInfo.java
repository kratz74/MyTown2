package mytown.commands.town.everyone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mytown.Formatter;
import mytown.MyTown;
import mytown.core.ChatUtils;
import mytown.core.utils.command.CommandBase;
import mytown.core.utils.command.Permission;
import mytown.datasource.MyTownDatasource;
import mytown.entities.Resident;
import mytown.entities.comparator.TownComparator;
import mytown.entities.town.Town;
import mytown.proxies.DatasourceProxy;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

@Permission("mytown.cmd.outsider.info")
public class CmdInfo extends CommandBase {

	public static int messageLength = 3;// Number of lines of info for each town

	public CmdInfo(String name, CommandBase parent) {
		super(name, parent);
	}

	@Override
	public void process(ICommandSender sender, String[] args) throws Exception {
		Resident res = getDatasource().getResident(sender.getCommandSenderName());
		String[] msg = null; // The whole message. Can have multiple town infos

		if (args.length < 1) {
			if (res.getSelectedTown() != null) {
				msg = prepare(res.getSelectedTown());
			} else
				throw new CommandException(MyTown.getLocal().getLocalization("mytown.cmd.err.info.notpart"));
		}

		if (args.length >= 1) {
			// Printing out info for all towns.
			if (args[0].equals("@a")) {

				List<Town> temp = new ArrayList<Town>(getDatasource().getTowns(false));

				// Using Comparator object to compare names and such
				TownComparator comp = new TownComparator(TownComparator.Order.Name);
				Collections.sort(temp, comp);
				msg = prepare(temp.toArray(new Town[temp.size()]));
			} else if (getDatasource().hasTown(args[0])) {
				msg = prepare(getDatasource().getTown(args[0]));
			} else
				throw new CommandException(MyTown.getLocal().getLocalization("mytown.cmd.err.town.notexist", args[0]));
		}
		for (int i = 0; i < msg.length / CmdInfo.messageLength; i++) {
			ChatUtils.sendLocalizedChat(sender, MyTown.getLocal(), "mytown.notification.town.info", msg[i * CmdInfo.messageLength], msg[i * CmdInfo.messageLength + 1], msg[i * CmdInfo.messageLength + 2]);
		}
	}

	public String[] prepare(Town... towns) {
		String[] msg = new String[towns.length * CmdInfo.messageLength];
		int i = 0;
		for (Town t : towns) {
			msg[i * 3] = EnumChatFormatting.BLUE + " ---------- " + t.getName() + EnumChatFormatting.GREEN + " (" + EnumChatFormatting.WHITE + "R:" + t.getResidents().size() + EnumChatFormatting.GREEN + " | " + EnumChatFormatting.WHITE + "B:" + t.getTownBlocks().size() + EnumChatFormatting.GREEN + " | " + EnumChatFormatting.WHITE + "P:" + t.getTownPlots().size() + EnumChatFormatting.GREEN + ")" + EnumChatFormatting.BLUE + " ----------" + '\n' + EnumChatFormatting.GRAY;
			msg[i * 3 + 1] = Formatter.formatResidentsToString(t.getResidents(), t) + '\n' + EnumChatFormatting.GRAY;
			msg[i++ * 3 + 2] = Formatter.formatRanksToString(t.getRanks());
		}
		return msg;
	}

	/**
	 * Helper method to return the current MyTownDatasource instance
	 * 
	 * @return
	 */
	private MyTownDatasource getDatasource() {
		return DatasourceProxy.getDatasource();
	}
}
