log_message() {
  echo "$1" >> "/sdcard/NikGapps/NikGapps_logs.log"
}

get_derived_path() {
    eval derived_path=\$${1}_derived_path
    echo $derived_path
}

RemoveAospAppsFromRom() {
    folders_that_exists=""
    for i in $(find "$system_derived_path" "$product_derived_path" "$system_ext_derived_path" -iname "$1" 2>/dev/null;); do
        if [ -d "$i" ]; then
            folders_that_exists="$folders_that_exists":"$i"
        fi
    done
    echo "$folders_that_exists"
}

set_perm() {
  chown "$1:$2" "$4"
  chmod "$3" "$4"
}

ch_con() {
  chcon -h u:object_r:"${1}"_file:s0 "$2"
}

update_prop() {
  dataPath=$1
  dataType=$2
  propFilePath="/system/etc/permissions/$3.prop"
  if [ ! -f "$propFilePath" ]; then
    touch "$propFilePath"
    log_message "- Creating $propFilePath"
  fi
  dataTypePath=$(echo "$dataPath" | sed "s|^$system/||")
  dataTypePath=${dataTypePath#/}
  line=$(grep -xn "$dataType=$dataTypePath" "$propFilePath" | cut -d: -f1)
  if [ -z "$line" ]; then
    echo "$dataType=$dataTypePath" >> "$propFilePath"
    log_message "- $dataType=$dataTypePath >> $propFilePath"
  else
    log_message "- $dataTypePath $dataType-ed already in $propFilePath"
  fi
}

install_file(){
  blank=""
  file_location=$(echo "$1" | sed "s/___/$blank/" | sed "s/___/\//g")
  package_title="$2"
  enforced_partition=$(echo "$file_location" | cut -d'/' -f 1)
  case "$enforced_partition" in
    "system"|"system_ext"|"product"|"vendor")
      log_message "- /$file_location is forced to be installed in $enforced_partition"
      destination_location="/$file_location"
      installPath=$(echo "$file_location" | sed "s/$enforced_partition\///")
    ;;
    "overlay")
      overlay_partition="/system/product"
      [ -n "$PRODUCT_BLOCK" ] && overlay_partition="/product"
      log_message "- /$file_location is forced to be installed in $overlay_partition"
      destination_location="$overlay_partition/$file_location"
      installPath="product/$file_location"
    ;;
    *)
      destination_location="$install_partition/$file_location"
      case "$install_partition" in
        *"/product") installPath="product/$file_location" ;;
        *"/system_ext") installPath="system_ext/$file_location" ;;
        *) installPath="$file_location" ;;
      esac
    ;;
  esac
  source_location="$source_directory/$1"
  if [ -f "$source_location" ]; then
    mkdir -p "$(dirname "$destination_location")"
    set_perm 0 0 0755 "$(dirname "$destination_location")"
    log_message "- Copying $source_location to $destination_location"
    cp -f "$source_location" "$destination_location"
    ch_con system "$destination_location"
    set_perm 0 0 0644 "$destination_location"
    update_prop "$installPath" "install" "$package_title"
  else
    log_message "- File $source_location not found"
    echo "File $source_location not found"
  fi
}

install_package(){
  title="$1"
  for k in $remove_aosp_apps_from_rom; do
      folders_to_delete="$(RemoveAospAppsFromRom "$k")"
      OLD_IFS=$IFS
      IFS=":"
      for i in $folders_to_delete; do
          if [ -n "$i" ]; then
              echo "- Deleting "$i""
              log_message "- Deleting $i"
              rm -rf "$i"
          fi
      done
      IFS="$OLD_IFS"
  done

  install_partition=$(get_derived_path $default_partition)

  for i in $file_list; do
    install_file "$i" "$title"
  done
}

install_package $title
