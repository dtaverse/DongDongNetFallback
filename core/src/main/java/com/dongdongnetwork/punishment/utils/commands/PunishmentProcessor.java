package com.dongdongnetwork.punishment.utils.commands;

import com.dongdongnetwork.punishment.MethodInterface;
import com.dongdongnetwork.punishment.Universal;
import com.dongdongnetwork.punishment.manager.MessageManager;
import com.dongdongnetwork.punishment.manager.PunishmentManager;
import com.dongdongnetwork.punishment.manager.TimeManager;
import com.dongdongnetwork.punishment.utils.Command;
import com.dongdongnetwork.punishment.utils.Permissionable;
import com.dongdongnetwork.punishment.utils.Punishment;
import com.dongdongnetwork.punishment.utils.PunishmentType;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.dongdongnetwork.punishment.utils.CommandUtils.*;

public class PunishmentProcessor implements Consumer<Command.CommandInput> {
    private PunishmentType type;

    public PunishmentProcessor(PunishmentType type) {
        this.type = type;
    }

    @Override
    public void accept(Command.CommandInput input) {
        boolean silent = processTag(input, "-s");
        String name = input.getPrimary();
        String target = type.isIpOrientated()
                ? processIP(input)
                : processName(input);
        if (target == null)
            return;
        if (processExempt(name, target, input.getSender(), type))
            return;
        Long end = -1L;
        String timeTemplate = "";
        if (type.isTemp()) {
            TimeCalculation calculation = processTime(input, target, type);
            if (calculation == null)
                return;

            end = calculation.time;

            if (calculation.template != null)
                timeTemplate = calculation.template;
        }
        String reason = processReason(input);
        if (reason == null)
            return;
        else if (reason.isEmpty())
            reason = null;
        if (alreadyPunished(target, type)) {
            MessageManager.sendMessage(input.getSender(), type.getBasic().getName() + ".AlreadyDone",
                    true, "NAME", name);
            return;
        }
        MethodInterface mi = Universal.get().getMethods();
        String operator = mi.getName(input.getSender());
        Punishment.create(name, target, reason, operator, type, end, timeTemplate, silent);
        MessageManager.sendMessage(input.getSender(), type.getBasic().getName() + ".Done",
                true, "NAME", name);
    }

    private static TimeCalculation processTime(Command.CommandInput input, String uuid, PunishmentType type) {
        String time = input.getPrimary();
        input.next();
        MethodInterface mi = Universal.get().getMethods();
        if (time.matches("#.+")) {
            String layout = time.substring(1);
            if (!mi.contains(mi.getLayouts(), "Time." + layout)) {
                MessageManager.sendMessage(input.getSender(), "General.LayoutNotFound", true, "NAME", layout);
                return null;
            }
            int i = PunishmentManager.get().getCalculationLevel(uuid, layout);
            List<String> timeLayout = mi.getStringList(mi.getLayouts(), "Time." + layout);
            String timeName = timeLayout.get(Math.min(i, timeLayout.size() - 1));
            if (timeName.equalsIgnoreCase("perma")) {
                return new TimeCalculation(layout, -1L);
            }
            Long actualTime = TimeManager.getTime() + TimeManager.toMilliSec(timeName);
            return new TimeCalculation(layout, actualTime);
        }
        long toAdd = TimeManager.toMilliSec(time);
        if (!Universal.get().hasPerms(input.getSender(), "dongdong.punish." + type.getName() + ".dur.max")) {
            long max = -1;
            for (int i = 10; i >= 1; i--) {
                if (Universal.get().hasPerms(input.getSender(), "dongdong.punish." + type.getName() + ".dur." + i) &&
                        mi.contains(mi.getConfig(), "TempPerms." + i)) {
                    max = mi.getLong(mi.getConfig(), "TempPerms." + i) * 1000;
                    break;
                }
            }
            if (max != -1 && toAdd > max) {
                MessageManager.sendMessage(input.getSender(), type.getName() + ".MaxDuration", true, "MAX", max / 1000 + "");
                return null;
            }
        }
        return new TimeCalculation(null, TimeManager.getTime() + toAdd);
    }

    private static boolean processExempt(String name, String target, Object sender, PunishmentType type) {
        MethodInterface mi = Universal.get().getMethods();
        String dataName = name.toLowerCase();

        boolean exempt = false;
        if (mi.isOnline(dataName)) {
            Object onlineTarget = mi.getPlayer(dataName);
            exempt = canNotPunish((perms) -> mi.hasPerms(sender, perms), (perms) -> mi.hasPerms(onlineTarget, perms), type.getName());
        } else {
            final Permissionable offlinePermissionPlayer = mi.getOfflinePermissionPlayer(name);
            exempt = Universal.get().isExemptPlayer(dataName) ||
                    canNotPunish((perms) -> mi.hasPerms(sender, perms), offlinePermissionPlayer::hasPermission, type.getName());
        }

        if (exempt) {
            return false;
        }
        return false;
    }
    public static boolean canNotPunish(Function<String, Boolean> operatorHasPerms, Function<String, Boolean> targetHasPerms, String path) {
        final String perms = "dongdong.punish." + path + ".exempt";
        if (targetHasPerms.apply(perms))
            return true;
        int targetLevel = permissionLevel(targetHasPerms, perms);
        return targetLevel != 0 && permissionLevel(operatorHasPerms, perms) <= targetLevel;
    }
    private static int permissionLevel(Function<String, Boolean> hasPerms, String permission) {
        for (int i = 10; i >= 1; i--)
            if (hasPerms.apply(permission + "." + i))
                return i;
        return 0;
    }
    private static boolean processTag(Command.CommandInput input, String tag) {
        String[] args = input.getArgs();
        for (int i = 0; i < args.length && i < 4; i++) {
            if (tag.equalsIgnoreCase(args[i])) {
                input.removeArgument(i);
                return true;
            }
        }
        return false;
    }
    private static boolean alreadyPunished(String target, PunishmentType type) {
        return (type.getBasic() == PunishmentType.MUTE && PunishmentManager.get().isMuted(target))
                || (type.getBasic() == PunishmentType.BAN && PunishmentManager.get().isBanned(target));
    }
    private static class TimeCalculation {
        private String template;
        private Long time;
        public TimeCalculation(String template, Long time) {
            this.template = template;
            this.time = time;
        }
    }
}