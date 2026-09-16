import { Box, Check, CircleDot, GitBranch, GitFork, Hourglass, Mail, Network, UserRoundCheck } from 'lucide-react'
import type { FloviraNodeType } from './types'

export const NODE_META: Record<FloviraNodeType, {
  label: string
  icon: typeof CircleDot
  tone: string
  color: string
}> = {
  '0': { label: '开始', icon: CircleDot, tone: 'start', color: '#60a5fa' },
  '1': { label: '审批', icon: UserRoundCheck, tone: 'approval', color: '#ff943e' },
  '2': { label: '结束', icon: Check, tone: 'end', color: '#60a5fa' },
  '3': { label: '条件分支', icon: GitBranch, tone: 'exclusive', color: '#6366f1' },
  '4': { label: '并行分支', icon: GitFork, tone: 'parallel', color: '#2563eb' },
  '5': { label: '多选分支', icon: Network, tone: 'inclusive', color: '#6366f1' },
  '6': { label: '子流程', icon: Box, tone: 'subprocess', color: '#a855f7' },
  '7': { label: '等待', icon: Hourglass, tone: 'wait', color: '#34d399' },
  '8': { label: '抄送', icon: Mail, tone: 'copy', color: '#06b6d4' },
}
