#!/system/bin/sh

CURRENT_DATE_TIME=$(date +%Y_%m_%d_%H_%M_%S)
DYNAMIC_PARTITIONS=false
ACTIVE_SLOT=""
APP_LOGS_DIR="/sdcard/NikGapps/app_logs"
LOG_FILE="$APP_LOGS_DIR/NikGapps_logs.log"
FLAG_FILE="$APP_LOGS_DIR/NikGapps_flags.log"