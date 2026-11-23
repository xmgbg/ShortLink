export function reportCsv(group, range, daily, ranking) {
  const rows = [
    ['分组', group], ['统计区间', ...range], [],
    ['日期', '访问次数（PV）', '独立访客（UV）', '独立 IP 数'],
    ...daily.map(row => [row.date, row.pv, row.uv, row.uip]), [],
    ['排名', '短链名称', '短链地址', '访问次数'],
    ...ranking.map((row, i) => [i + 1, row.title, row.fullShortUrl, row.pv])
  ]
  function cell(value) {
    let text = String(value ?? '')
    if (/^\s*[=+@-]|^[\t\r\n]/.test(text)) text = "'" + text
    return '"' + text.replaceAll('"', '""') + '"'
  }
  return '\uFEFF' + rows.map(row => row.map(cell).join(',')).join('\r\n')
}
