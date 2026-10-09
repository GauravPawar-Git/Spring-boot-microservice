{{/* Labels every object gets: who manages it and which release it belongs to */}}
{{- define "ms.labels" -}}
app.kubernetes.io/part-of: microservices
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end }}

{{/* Selector labels: must never change after install, so only the component name */}}
{{- define "ms.selector" -}}
app.kubernetes.io/name: {{ . }}
{{- end }}
