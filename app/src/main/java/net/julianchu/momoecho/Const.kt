package net.julianchu.momoecho

class Const {
    companion object {
        const val TAG = "momoecho"

        const val PREF_KEY_TRACK_ID = "current_track_id"
        const val PREF_KEY_PERIOD = "period"

        const val COMMAND_PLAYBACK_ONE_CLIP = "_playback_one_clip_"
        const val COMMAND_GET_INFO = "_get_data_source_info_"
        const val COMMAND_REMOVE_CLIP = "_remove_clip_"
        const val COMMAND_SET_TRACK = "_set_track_"
        const val COMMAND_REFRESH_CLIP = "_refresh_clip_"
        const val COMMAND_UPDATE_CLIP = "_update_clip_"
        const val EVENT_UPDATE_INFO = "_event_update_data_source_info_"
        const val EVENT_UPDATE_CLIP = "_event_update_clip_"
        const val EVENT_UPDATE_CLIPS = "_event_update_clips_"
        const val EXTRA_KEY_TRACK = "_track_id_in_bundle_"
        const val EXTRA_KEY_CLIP = "_clip_in_bundle_"
        const val EXTRA_KEY_INFO_DURATION = "_info_duration_"
    }
}