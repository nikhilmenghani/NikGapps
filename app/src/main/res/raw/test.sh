#!/system/bin/sh

. $(dirname "$0")/NikGapps_flags.log

# Define an array of partition names
partitions="system product system_ext"

get_mount_point() {
    eval mount_point=\$${1}_mount_point
    echo $mount_point
}

get_derived_path() {
    eval derived_path=\$${1}_derived_path
    echo $derived_path
}

get_free_space() {
    partition_name="$1"
    df_value=$(df -k $(get_mount_point $partition_name) | tail -n 1 | awk '{print $4}')
    echo $df_value
}

get_total_space() {
    partition_name="$1"
    df_value=$(df -k $(get_mount_point $partition_name) | tail -n 1 | awk '{print $2}')
    echo $df_value
}

get_df_output() {
    partition_name="$1"
    mount_point=$2
    [ -z "$mount_point" ] && mount_point=$(get_mount_point $partition_name)
    df $mount_point | tail -n 1
}

print_size() {
  partition="$1"
  mount_point=$(get_mount_point $partition)
  df=$(df -k "$mount_point" | tail -n 1)
  file_system=$(echo $df | awk '{print $1}')
  total_space=$(echo $df | awk '{print $2}')
  used_space=$(echo $df | awk '{print $3}')
  free_space=$(echo $df | awk '{print $4}')
  used_space_percent=$(echo $df | awk '{print $5}')
  mounted_on=$(echo $df | awk '{print $6}')
  printf "%12s | %9s | %9s | %9s | %7s\n" "$mount_point" "$total_space" "$used_space" "$free_space" "$used_space_percent"
}

print_header() {
  printf "%12s | %9s | %9s | %9s | %7s\n" "Mount Point" "Total" "Used" "Free" "Use%"
  printf "%12s | %9s | %9s | %9s | %7s\n" "------------" "---------" "---------" "---------" "-----"
}

for partition in $partitions; do
    echo "- Partition: $partition"
    echo "- Free Space: $(get_free_space $partition)"
done