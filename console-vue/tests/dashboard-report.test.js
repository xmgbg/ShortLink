import test from 'node:test'
import assert from 'node:assert/strict'
import { reportCsv } from '../src/views/dashboard/report.js'

test('导出保留中文、引号和换行，用户输入的表格公式按文本导出', () => {
  const csv = reportCsv('默认分组', ['2026-11-15', '2026-11-21'],
    [{ date: '2026-11-16', pv: 20, uv: 8, uip: 7 }],
    [{ title: '=测试,"标题"\n第二行', fullShortUrl: 'xmgbg.top/demo', pv: 20 }])
  assert.ok(csv.startsWith('\uFEFF'))
  assert.ok(csv.includes('"2026-11-16","20","8","7"'))
  assert.ok(csv.includes(`"1","'=测试,""标题""\n第二行","xmgbg.top/demo","20"`))
})
