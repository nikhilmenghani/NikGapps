#!/system/bin/sh

cd "$(dirname "$0")"
. ./variables.sh

set_perm() {
  chown "$1:$2" "$4"
  chmod "$3" "$4"
}

# copy the addon.d script to the correct location
if [ -f $APP_LOGS_DIR/$1 ]; then
  mkdir -p /system/addon.d
  echo "Copying addon script to /system/addon.d"
  cp $APP_LOGS_DIR/$1 /system/addon.d/$1
  set_perm 0 0 0755 /system/addon.d/$1
else
  echo "Addon script not found"
fi
