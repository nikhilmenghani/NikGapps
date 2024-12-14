#!/system/bin/sh

generate_filename() {
  directory="$1"; prefix="$2"; app_name="$3"
  max_index=0
  for file in "$directory"/"$prefix"-[0-9]*-"$app_name".sh; do
    [ -e "$file" ] || continue
    index=$(basename "$file" | cut -d '-' -f 2 | sed 's/[^0-9]*//g')
    if [ "$index" -eq "$index" ] 2>/dev/null; then
      echo "$file"
      return
    fi
  done
  for file in "$directory"/*.sh; do
    if grep -q "AFZC" "$file"; then
      file_name=$(basename "$file")
      file_constant=$(echo "$file_name" | cut -d '-' -f 1)
      if [ "$file_constant" = "$prefix" ]; then
        index=$(echo "$file_name" | cut -d '-' -f 2 | sed 's/[^0-9]*//g')
        if [ -n "$index" ] && [ "$index" -eq "$index" ] 2>/dev/null; then
          [ "$index" -gt "$max_index" ] && max_index="$index"
        fi
      fi
    fi
  done
  max_index="${max_index#0}"
  max_index=$((max_index + 1))
  new_file="$prefix-$(printf "%02d" "$max_index")-$app_name.sh"
  echo "$new_file"
}

echo $(generate_filename "/system/addon.d" "$1" "$2")