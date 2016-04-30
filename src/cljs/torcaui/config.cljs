(ns torcaui.config)

(defn logging-enabled?
  []
  "If true, log statements print to the browswer's JavaScript console."
  (boolean (aget js/window "renderContext" "logging_enabled")))

(defn log-channels?
  "If true, log all messages on global core.async channels."
  []
  (boolean (aget js/window "renderContext" "log_channels")))