#!/bin/bash
# Debloat Motorola Moto G Play 2024 (XT2413-1) for Snowy robot pet project
# Removes carrier bloatware, games, social media, ads, and unnecessary Motorola apps
# Keeps: camera, WiFi, Bluetooth, Play Store, Play Services (for ML Kit), Settings, keyboard, WebView
#
# Usage: bash scripts/debloat-motorola.sh
# Safe: uses pm uninstall --user 0 (per-user uninstall, reversible with factory reset)

set -euo pipefail

# Check ADB connection
if ! adb devices | grep -q "device$"; then
    echo "ERROR: No device connected via ADB. Connect the phone and enable USB debugging."
    exit 1
fi

echo "=== Snowy Debloat Script ==="
echo "Device: $(adb shell getprop ro.product.model)"
echo "Android: $(adb shell getprop ro.build.version.release)"
echo ""

remove_package() {
    local pkg="$1"
    local desc="$2"
    if adb shell pm list packages | grep -q "^package:${pkg}$"; then
        echo "  Removing: $pkg ($desc)"
        adb shell pm uninstall -k --user 0 "$pkg" 2>/dev/null || \
        adb shell pm disable-user --user 0 "$pkg" 2>/dev/null || \
        echo "    WARNING: Could not remove $pkg"
    fi
}

# ============================================================
# GAMES
# ============================================================
echo "--- Removing games ---"
remove_package "com.vitastudio.mahjong" "Mahjong"
remove_package "com.superplaystudios.dicedreams" "Dice Dreams"
remove_package "in.playsimple.triple.tile.matchgame.pair.three.puzzle.object.royale" "Triple Tile Match"
remove_package "com.block.juggle" "Block Juggle"
remove_package "ball.sort.puzzle.color.sorting.bubble.games" "Ball Sort Puzzle"
remove_package "com.mobilityware.solitaire" "Solitaire"
remove_package "com.soulcompany.bubbleshooter.relaxing" "Bubble Shooter"
remove_package "com.oakever.tiletrip" "Tile Trip"
remove_package "com.tripledot.woodoku" "Woodoku"
remove_package "in.playsimple.wordtrip" "Word Trip"

# ============================================================
# SOCIAL MEDIA / FACEBOOK
# ============================================================
echo "--- Removing social media ---"
remove_package "com.facebook.appmanager" "Facebook App Manager"
remove_package "com.facebook.system" "Facebook System"
remove_package "com.facebook.services" "Facebook Services"

# ============================================================
# NEWS / WEATHER (third-party)
# ============================================================
echo "--- Removing news and weather apps ---"
remove_package "com.novanews.localnews.en" "Nova News"
remove_package "com.particlenews.newsbreak" "NewsBreak"
remove_package "com.localweather.radar.climate" "Local Weather Radar"
remove_package "com.inmobi.weather" "InMobi Weather"
remove_package "com.handmark.expressweather" "Tracfone Weather"

# ============================================================
# SHOPPING / FINANCE
# ============================================================
echo "--- Removing shopping and finance apps ---"
remove_package "com.amazon.mShop.android.shopping" "Amazon Shopping"
remove_package "com.amazon.appmanager" "Amazon App Manager"
remove_package "com.acorns.android" "Acorns"
remove_package "com.onedebit.chime" "Chime"
remove_package "com.freecash.app2" "FreeCash"

# ============================================================
# CRICKET / AT&T CARRIER BLOATWARE
# ============================================================
echo "--- Removing Cricket/AT&T bloatware ---"
remove_package "com.dti.cricket" "Cricket App"
remove_package "com.cricketwireless.thescoop" "Cricket The Scoop"
remove_package "com.cricketwireless.minus" "Cricket Minus One"
remove_package "com.digitalturbine.cricketbar" "Digital Turbine Cricket"
remove_package "com.mizmowireless.acctmgt" "Cricket Account Mgmt"
remove_package "com.att.dh" "AT&T Device Help"
remove_package "com.att.mobile.android.vvm" "AT&T Visual Voicemail"
remove_package "com.att.personalcloud" "AT&T Personal Cloud"
remove_package "com.att.csoiam.mobilekey" "AT&T Mobile Key"
remove_package "com.att.deviceunlock" "AT&T Device Unlock"
remove_package "com.att.iqi" "AT&T IQI Analytics"
remove_package "com.motorola.omadm.att" "AT&T Device Management"
remove_package "com.motorola.att.phone.extensions" "AT&T Phone Extensions"
remove_package "com.motorola.att.sim.extensions" "AT&T SIM Extensions"
remove_package "com.motorola.attvowifi" "AT&T VoWiFi"
remove_package "com.aura.oobe.cricket" "Aura OOBE Cricket"
remove_package "com.aura.appadvisor.att" "Aura App Advisor AT&T"
remove_package "com.aura.appadvisor.cricket" "Aura App Advisor Cricket"
remove_package "com.aura.jet.att" "Aura Jet AT&T"

# ============================================================
# VERIZON BLOATWARE (inexplicably pre-installed)
# ============================================================
echo "--- Removing Verizon bloatware ---"
remove_package "com.vzw.hss.myverizon" "My Verizon"
remove_package "com.vcast.mediamanager" "Verizon Cloud"
remove_package "com.vzw.apnlib" "Verizon APN Lib"
remove_package "com.vzw.apnservice" "Verizon APN Service"
remove_package "com.vzw.ecid" "Verizon ECID"
remove_package "com.verizon.llkagent" "Verizon LLK Agent"
remove_package "com.verizon.loginengine.unbranded" "Verizon Login Engine"
remove_package "com.verizon.mips.services" "Verizon MVS"
remove_package "com.verizon.obdm" "Verizon OBDM"
remove_package "com.verizon.obdm_permissions" "Verizon OBDM Permissions"
remove_package "com.securityandprivacy.android.verizon.vms" "Verizon Security"
remove_package "com.motorola.omadm.vzw" "Verizon Device Management"
remove_package "com.motorola.vzw.cloudsetup" "Verizon Cloud Setup"
remove_package "com.motorola.vzw.pco.extensions.pcoreceiver" "Verizon PCO"
remove_package "com.motorola.vzw.phone.extensions" "Verizon Phone Extensions"
remove_package "com.motorola.vzw.settings.extensions" "Verizon Settings Extensions"
remove_package "com.motorola.vzw.provider" "Verizon Unified Settings"
remove_package "com.motorola.setupwizard.controller" "Verizon Setup Controller"
remove_package "com.motorola.setupwizard.devicesetup" "Verizon Device Setup"
remove_package "com.motorola.setupwizard.phoneservice" "Verizon Phone Service Setup"
remove_package "com.customermobile.preload.vzw" "Verizon Preload"

# ============================================================
# OTHER CARRIER BLOATWARE (Sprint, T-Mobile, Tracfone, Dish, Comcast, Spectrum, USCC)
# ============================================================
echo "--- Removing other carrier bloatware ---"
remove_package "com.motorola.settings" "Sprint Settings"
remove_package "com.motorola.sprint.setupext" "Sprint Setup"
remove_package "com.motorola.carrierconfig" "T-Mobile Config"
remove_package "com.tmobile.echolocate.system" "T-Mobile Traffic Stats"
remove_package "com.swishme.tracfone" "Tracfone Dashboard"
remove_package "com.tracfone.generic.mysites" "Tracfone MySites"
remove_package "com.tracfone.preload.accountservices" "Tracfone Device Pulse"
remove_package "com.motorola.tracfone.rsu" "Tracfone RSU"
remove_package "com.motorola.extensions.tracfone" "Tracfone Unlock"
remove_package "com.aura.oobe.dish" "Dish App Cloud"
remove_package "com.dish.wireless.carrierapp" "Dish Carrier App"
remove_package "com.dish.wireless.installer" "Dish Installer"
remove_package "com.dish.vvm" "Dish VVM"
remove_package "com.motorola.omadm.dish" "Dish Device Management"
remove_package "com.xfinitymobile.cometcarrierservice" "Comcast Carrier"
remove_package "com.motorola.comcastext" "Comcast Extension"
remove_package "com.motorola.comcast.settings.extensions" "Comcast Settings"
remove_package "com.spectrum.cm.headless" "Spectrum CM"
remove_package "com.motorola.spectrum.setup.extensions" "Spectrum Setup"
remove_package "com.uscc.ecid" "US Cellular Call Guardian"
remove_package "cci.usage" "CCI Usage"

# ============================================================
# ADWARE / TRACKING / DIGITAL TURBINE / AURA
# ============================================================
echo "--- Removing adware and tracking ---"
remove_package "com.adfone.aditup" "Adfone Aditup"
remove_package "com.aura.oobe.solutions" "Aura OOBE Solutions"
remove_package "com.aura.oobe.motorola" "Aura OOBE Motorola"
remove_package "com.inmobi.installer" "InMobi Installer"
remove_package "com.spynet.camon" "SpyNet CamOn"
remove_package "com.glance.lockscreenM" "Glance Lock Screen"
remove_package "com.google.android.adservices.api" "Google Ad Services"
remove_package "com.google.mainline.adservices" "Google Mainline Ad Services"
remove_package "com.google.mainline.telemetry" "Google Telemetry"
remove_package "com.google.android.gms.location.history" "Google Location History"

# ============================================================
# MOTOROLA BLOATWARE (non-essential)
# ============================================================
echo "--- Removing non-essential Motorola apps ---"
remove_package "com.motorola.moto" "Moto App"
remove_package "com.motorola.mototour" "Moto Tour"
remove_package "com.motorola.help" "Moto Help"
remove_package "com.motorola.help.extlog" "Moto Feedback"
remove_package "com.motorola.motocare" "Moto Care"
remove_package "com.motorola.genie" "Guide Me"
remove_package "com.motorola.gamemode" "Moto Game Mode"
remove_package "com.motorola.actions" "Moto Actions"
remove_package "com.motorola.spaces" "Moto Spaces"
remove_package "com.motorola.timeweatherwidget" "Moto Time/Weather Widget"
remove_package "com.motorola.audiorecorder" "Moto Audio Recorder"
remove_package "com.motorola.appforecast" "Moto App Forecast"
remove_package "com.motorola.ccc.notification" "Moto Notifications"
remove_package "com.motorola.ccc.mainplm" "Moto 3C Main"
remove_package "com.motorola.ccc.devicemanagement" "Moto 3C Device Management"
remove_package "com.motorola.ccc.ota" "Moto OTA"
remove_package "com.motorola.discovery" "Moto Feature Discovery"
remove_package "com.motorola.personalize" "Moto Personalize"
remove_package "com.motorola.demo" "Moto Demo Mode"
remove_package "com.motorola.dimo" "Moto Digital Account"
remove_package "com.motorola.om" "Moto Om"
remove_package "com.motorola.brapps" "BR Apps"
remove_package "com.motorola.bug2go" "Bug2Go"
remove_package "com.motorola.securityhub" "Moto Security Hub"
remove_package "com.motorola.securityhubext" "Moto Security Hub Ext"
remove_package "com.motorola.paks" "PAKS"
remove_package "com.motorola.paks.notification" "PAKS Notification"
remove_package "com.motorola.slpc_sys" "SLPC System"
remove_package "com.motorola.fmplayer" "FM Player"
remove_package "com.motorola.wappush" "WAP Push"
remove_package "com.motorola.rcs.eab" "RCS EAB"
remove_package "com.motorola.rcsConfigService" "RCS Config"
remove_package "com.motorola.visualvoicemail" "Visual Voicemail"
remove_package "com.motorola.omadm.service" "DM Service"
remove_package "com.motorola.android.provisioning" "OMA Provisioning"
remove_package "com.motorola.android.fota" "Moto FOTA"
remove_package "com.motorola.iqimotmetrics" "IQI Mot Metrics"
remove_package "com.motorola.lifetimedata" "Lifetime Data"
remove_package "com.motorola.bach.modemstats" "Modem Stats"
remove_package "com.motorola.wifi.motowifimetrics" "WiFi Metrics"
remove_package "com.motorola.dciservice" "DCI Stats"
remove_package "com.motorola.obdmapi.service" "OBDM Service"
remove_package "com.motorola.callredirectionservice" "Call Redirection"
remove_package "com.motorola.entitlement" "Entitlement"
remove_package "com.motorola.securevault" "Secure Vault"
remove_package "com.motorola.hce" "HCE Service"
remove_package "com.motorola.motosignature2.app" "Moto Signature 2"
remove_package "com.motorola.motosignature.app" "Moto Signature"
remove_package "com.motorola.launcherconfig" "Launcher Config"
remove_package "com.motorola.camera3.content.ai" "Camera AI Content"
remove_package "com.lenovo.lsf.user" "Lenovo ID"
remove_package "udc.lenovo.com.udclient" "Lenovo UDC"

# ============================================================
# GOOGLE APPS (non-essential for robot pet)
# ============================================================
echo "--- Removing non-essential Google apps ---"
remove_package "com.google.android.youtube" "YouTube"
remove_package "com.google.android.apps.youtube.music" "YouTube Music"
remove_package "com.google.android.apps.youtube.music.setupwizard" "YT Music Setup"
remove_package "com.google.android.apps.maps" "Google Maps"
remove_package "com.google.android.apps.photos" "Google Photos"
remove_package "com.google.android.apps.docs" "Google Drive"
remove_package "com.google.android.apps.tachyon" "Google Meet"
remove_package "com.google.android.gm" "Gmail"
remove_package "com.google.android.calendar" "Google Calendar"
remove_package "com.google.android.videos" "Google Videos"
remove_package "com.google.android.apps.wallpaper" "Google Wallpapers"
remove_package "com.google.android.apps.walletnfcrel" "Google Wallet"
remove_package "com.google.android.apps.wellbeing" "Digital Wellbeing"
remove_package "com.google.android.apps.safetyhub" "Personal Safety"
remove_package "com.google.android.apps.turbo" "Device Health Services"
remove_package "com.google.android.apps.chromecast.app" "Google Home"
remove_package "com.google.android.apps.adm" "Find My Device"
remove_package "com.google.android.apps.restore" "Google Restore"
remove_package "com.google.android.apps.nbu.files" "Files by Google"
remove_package "com.google.android.apps.messaging" "Google Messages"
remove_package "com.google.android.projection.gearhead" "Android Auto"
remove_package "com.google.android.apps.googleassistant" "Google Assistant"
remove_package "com.google.android.googlequicksearchbox" "Google Search/Assistant"
remove_package "com.google.android.apps.scone" "SCONE"
remove_package "com.google.android.deskclock" "Google Clock"
remove_package "com.google.android.calculator" "Google Calculator"
remove_package "com.google.android.contacts" "Google Contacts"
remove_package "com.google.android.dialer" "Google Dialer"
remove_package "com.google.android.marvin.talkback" "TalkBack"
remove_package "com.google.android.tts" "Google TTS"
remove_package "com.google.android.apps.carrier.carrierwifi" "Google Carrier WiFi"
remove_package "com.google.android.wfcactivation" "WFC Activation"
remove_package "com.google.android.gms.supervision" "Family Link"
remove_package "com.google.android.feedback" "Google Feedback"
remove_package "com.google.android.printservice.recommendation" "Print Recommendation"
remove_package "com.google.android.contactkeys" "Contact Keys"
remove_package "com.google.android.apps.cbrsnetworkmonitor" "CBRS Network Monitor"

# ============================================================
# MISC APPS WE DON'T NEED
# ============================================================
echo "--- Removing misc unnecessary apps ---"
remove_package "com.pandora.android" "Pandora"
remove_package "com.pdfreader.free.viewer.documentreader" "PDF Reader"
remove_package "com.dti.folderlauncher" "Folder Launcher"
remove_package "com.motorola.easyprefix" "Easy Prefix"
remove_package "com.motorola.gesture" "Gesture Tutorial"
remove_package "com.dolby.daxservice" "Dolby DAX Service"
remove_package "com.motorola.dolby.dolbyui" "Dolby UI"
remove_package "com.bluetooth.aptxmode" "Bluetooth AptX"
remove_package "com.android.chrome" "Chrome"
remove_package "com.motorola.android.providers.chromehomepage" "Chrome Homepage Provider"
remove_package "com.android.egg" "Easter Egg"
remove_package "com.android.dreams.basic" "Basic Dreams"
remove_package "com.android.printspooler" "Print Spooler"
remove_package "com.android.bips" "Built-in Print"
remove_package "com.android.bookmarkprovider" "Bookmark Provider"
remove_package "com.android.stk" "SIM Toolkit"
remove_package "com.android.cellbroadcastreceiver" "Cell Broadcast"
remove_package "android.autoinstalls.config.motorola.layout" "Auto Install Config"
remove_package "com.motorola.android.buacloudcontactadapter" "Cloud Contact Adapter"
remove_package "com.motorola.contacts.preloadcontacts" "Preload Contacts"
remove_package "com.thundercomm.ar.core" "AR Core Service"
remove_package "com.motorola.revoker.services" "Revoker Services"

echo ""
echo "=== Debloat complete ==="
echo ""
echo "Remaining packages:"
adb shell pm list packages | wc -l
echo ""
echo "Next steps:"
echo "  1. Reboot the phone: adb reboot"
echo "  2. Set screen timeout to max in Settings > Display"
echo "  3. Enable 'Stay awake while charging' in Developer Options"
echo "  4. Disable auto-updates in Play Store settings"
echo "  5. Connect to home WiFi"
