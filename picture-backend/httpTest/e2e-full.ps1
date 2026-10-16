param(
  [string]$Base = 'http://127.0.0.1:8123/api',
  [string]$ImagePath = 'F:\Code\final_gra\picture-backend\httpTest\11.png',
  [string]$Mysql = 'C:\Program Files\MySQL\MySQL Server 9.1\bin\mysql.exe',
  [string]$OutDir = 'F:\Code\final_gra\picture-backend\httpTest'
  ,[string]$ExifImagePath = 'F:\Code\final_gra\picture-backend\httpTest\e2e-exif.jpg'
)

$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'
$script:Results = New-Object System.Collections.Generic.List[object]
$script:LastData = $null
$ts = [int][double]::Parse((Get-Date -UFormat %s))
$Session = @{}
$Ctx = @{}
$Current = ''

function J($o) { return ($o | ConvertTo-Json -Depth 8 -Compress) }

function Run-Sql([string]$sql) {
  return (& $Mysql -uroot -proot -D smart_photo_community -N -B -e $sql 2>$null)
}

function Record([string]$Name, [string]$Expect, $Code, [string]$Msg, [bool]$Pass) {
  $script:Results.Add([pscustomobject]@{ name=$Name; expect=$Expect; code="$Code"; pass=$Pass; message=$Msg })
  $tag = 'FAIL'
  if ($Pass) { $tag = 'PASS' }
  $line = '[' + $tag + '] ' + $Name + ' | code=' + "$Code" + ' | ' + $Msg
  if ($Pass) { Write-Host $line } else { Write-Host $line -ForegroundColor Red }
}

function Test-Api {
  param(
    [Parameter(Mandatory=$true)][string]$Name,
    [string]$Method = 'GET',
    [Parameter(Mandatory=$true)][string]$Path,
    $Body = $null,
    [string]$As = 'A',
    [hashtable]$Form = $null,
    [ValidateSet('ok','fail')][string]$Expect = 'ok',
    [scriptblock]$Check = $null,
    [switch]$RawStatus
  )
  $uri = $Base + $Path
  $p = @{ Uri = $uri; Method = $Method; TimeoutSec = 120 }
  if ($As -ne 'NONE' -and $Session.ContainsKey($As)) { $p.WebSession = $Session[$As] }
  if ($null -ne $Body) { $p.ContentType = 'application/json'; $p.Body = (J $Body) }
  if ($null -ne $Form) { $p.Form = $Form }

  $code = 'null'; $msg = ''; $data = $null
  if ($RawStatus) {
    try {
      $r = Invoke-WebRequest @p
      $code = [string]$r.StatusCode
      $msg = 'bytes=' + $r.RawContentLength
      $pass = ($Expect -eq 'ok' -and $code -eq '200')
      Record $Name $Expect $code $msg $pass
      return
    } catch {
      Record $Name $Expect 'EXC' $_.Exception.Message $false
      return
    }
  }

  try {
    $resp = Invoke-RestMethod @p
    $code = [string]$resp.code
    $msg = [string]$resp.message
    $data = $resp.data
    $script:LastData = $data
  } catch {
    $code = 'EXC'
    $msg = $_.Exception.Message
    $script:LastData = $null
  }

  $pass = $false
  if ($Expect -eq 'ok') { $pass = ($code -eq '0') } else { $pass = ($code -ne '0') }
  if ($pass -and $Check) {
    try {
      $ok = & $Check $script:LastData
      if (-not $ok) { $pass = $false; $msg = $msg + ' | assertion failed' }
    } catch {
      $pass = $false; $msg = $msg + ' | assertion exception: ' + $_.Exception.Message
    }
  }
  Record $Name $Expect $code $msg $pass
}

function New-UserSession([string]$Key, [string]$Account, [string]$Password) {
  $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
  $Session[$Key] = $s
  Test-Api -Name ('register ' + $Key) -Method POST -Path '/user/register' -Body @{ userAccount=$Account; userPassword=$Password; checkPassword=$Password } -As NONE
  Test-Api -Name ('login ' + $Key) -Method POST -Path '/user/login' -Body @{ userAccount=$Account; userPassword=$Password } -As $Key -Check { param($d) $d.userAccount -eq $Account }
  $Ctx[$Key + 'Id'] = "$($script:LastData.id)"
}

Write-Host '================ S1 认证与用户 ================' -ForegroundColor Cyan
$acctA = 'e2e_a_' + $ts + '@test.com'
$acctB = 'e2e_b_' + $ts + '@test.com'
$acctC = 'e2e_c_' + $ts + '@test.com'
$acctAdmin = 'e2e_admin_' + $ts + '@test.com'
$pwd = 'e2e123456'

New-UserSession 'A' $acctA $pwd
New-UserSession 'B' $acctB $pwd
New-UserSession 'C' $acctC $pwd

$s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$Session['ADMIN'] = $s
Test-Api -Name 'register ADMIN' -Method POST -Path '/user/register' -Body @{ userAccount=$acctAdmin; userPassword=$pwd; checkPassword=$pwd } -As NONE
Run-Sql ("update user set userRole='admin' where userAccount='" + $acctAdmin + "'") | Out-Null
Test-Api -Name 'login ADMIN (role=admin)' -Method POST -Path '/user/login' -Body @{ userAccount=$acctAdmin; userPassword=$pwd } -As ADMIN -Check { param($d) $d.userRole -eq 'admin' }

Test-Api -Name '未登录访问 /user/get/login' -Path '/user/get/login' -As NONE -Expect fail
Test-Api -Name 'profile/update A' -Method POST -Path '/user/profile/update' -Body @{ userName='E2E摄影师A'; userProfile='端到端测试账号' } -As A
$Ctx['aId'] = $Ctx['AId']

Write-Host '================ S2 图片上传（COS） ================' -ForegroundColor Cyan
Test-Api -Name 'space/add 私有空间 A' -Method POST -Path '/space/add' -Body @{ spaceName=('e2e私有_' + $ts); spaceType=0 } -As A -Check { param($d) $null -ne $d }
$Ctx['privateSpaceId'] = "$($script:LastData)"
Test-Api -Name 'space/add 团队空间 A' -Method POST -Path '/space/add' -Body @{ spaceName=('e2e团队_' + $ts); spaceType=1 } -As A -Check { param($d) $null -ne $d }
$Ctx['teamSpaceId'] = "$($script:LastData)"

Test-Api -Name 'picture/upload 上传到公共图库' -Method POST -Path '/picture/upload?picName=e2e_public_upload' -Form @{ file = Get-Item $ImagePath } -As A -Check { param($d) $d.processingStatus -eq 'SUCCEEDED' -and $d.url -like 'http*' }
$Ctx['picA'] = "$($script:LastData.id)"
$Ctx['picAUrl'] = "$($script:LastData.url)"
$Ctx['picAThumb'] = "$($script:LastData.thumbnailUrl)"
$Ctx['picATask'] = "$($script:LastData.uploadTaskId)"

foreach ($pair in @(@('原图', $Ctx['picAUrl']), @('缩略图', $Ctx['picAThumb']))) {
  try {
    $abs = Invoke-WebRequest -Uri $pair[1] -UseBasicParsing -TimeoutSec 30
    Record ($pair[0] + ' COS 地址可匿名访问(HTTP 200)') 'ok' ([string]$abs.StatusCode) ('bytes=' + $abs.RawContentLength) ($abs.StatusCode -eq 200)
  } catch {
    Record ($pair[0] + ' COS 地址可匿名访问(HTTP 200)') 'ok' 'EXC' $_.Exception.Message $false
  }
}
Test-Api -Name 'picture/upload/status 查询处理状态' -Path ('/picture/upload/status?taskId=' + $Ctx['picATask']) -As A -Check { param($d) $d.status -eq 'SUCCEEDED' }

Test-Api -Name 'picture/upload 上传到团队空间(owner)' -Method POST -Path ('/picture/upload?picName=e2e_team_upload&spaceId=' + $Ctx['teamSpaceId']) -Form @{ file = Get-Item $ImagePath } -As A -Check { param($d) $d.processingStatus -eq 'SUCCEEDED' -and "$($d.spaceId)" -eq $Ctx['teamSpaceId'] }
$Ctx['picTeam'] = "$($script:LastData.id)"

Test-Api -Name 'picture/upload 上传到私有空间(owner)' -Method POST -Path ('/picture/upload?picName=e2e_private_upload&spaceId=' + $Ctx['privateSpaceId']) -Form @{ file = Get-Item $ImagePath } -As A -Check { param($d) $d.processingStatus -eq 'SUCCEEDED' }
$Ctx['picPrivate'] = "$($script:LastData.id)"

Test-Api -Name 'picture/upload/url 通过 URL 上传' -Method POST -Path '/picture/upload/url' -Body @{ fileUrl=$Ctx['picAUrl']; picName='e2e_url_upload' } -As A
$Ctx['picUrlUpload'] = "$($script:LastData.id)"

Write-Host '================ S3 图片查询与审核 ================' -ForegroundColor Cyan
Test-Api -Name 'picture/get/vo 自己的图片' -Path ('/picture/get/vo?id=' + $Ctx['picA']) -As A -Check { param($d) "$($d.id)" -eq $Ctx['picA'] }
Test-Api -Name 'picture/list/page/vo 公共图库' -Method POST -Path '/picture/list/page/vo' -Body @{ current=1; pageSize=10 } -As B -Check { param($d) [int]$d.total -ge 1 }
Test-Api -Name 'admin picture/review 审核通过' -Method POST -Path '/picture/review' -Body @{ id=[long]$Ctx['picA']; reviewStatus=1; reviewMessage='e2e 自动审核' } -As ADMIN
Test-Api -Name '审核后公共图库可见自己的图片' -Method POST -Path '/picture/list/page/vo' -Body @{ current=1; pageSize=20 } -As B -Check { param($d) ($d.records | ForEach-Object { "$($_.id)" }) -contains $Ctx['picA'] }
Test-Api -Name 'picture/edit 本人编辑自己的图片' -Method POST -Path '/picture/edit' -Body @{ id=[long]$Ctx['picA']; name='e2e_renamed'; category='风景'; tags=@('e2e','测试') } -As A -Check { param($d) $d -eq $true }
Test-Api -Name 'picture/update 管理员修改图片信息' -Method POST -Path '/picture/update' -Body @{ id=[long]$Ctx['picA']; name='e2e_admin_renamed' } -As ADMIN -Check { param($d) $d -eq $true }
Test-Api -Name '编辑后审核状态重置为待审核(设计行为)' -Path ('/picture/get?id=' + $Ctx['picA']) -As ADMIN -Check { param($d) [int]$d.reviewStatus -eq 0 }
Test-Api -Name '重新审核通过' -Method POST -Path '/picture/review' -Body @{ id=[long]$Ctx['picA']; reviewStatus=1; reviewMessage='e2e 重新审核' } -As ADMIN -Check { param($d) $d -eq $true }
Test-Api -Name '重新审核后公共图库再次可见' -Method POST -Path '/picture/list/page/vo' -Body @{ current=1; pageSize=20 } -As B -Check { param($d) ($d.records | ForEach-Object { "$($_.id)" }) -contains $Ctx['picA'] }
Test-Api -Name '越权: B 编辑 A 的图片' -Method POST -Path '/picture/edit' -Body @{ id=[long]$Ctx['picA']; name='hacked_by_B' } -As B -Expect fail
Test-Api -Name '越权: B 删除 A 的图片' -Method POST -Path '/picture/delete' -Body @{ id=[long]$Ctx['picA'] } -As B -Expect fail
Test-Api -Name 'picture/upload 临时图(用于删除验证)' -Method POST -Path '/picture/upload?picName=e2e_temp_delete' -Form @{ file = Get-Item $ImagePath } -As A -Check { param($d) $d.processingStatus -eq 'SUCCEEDED' }
$tmpPic = "$($script:LastData.id)"
$Ctx['tmpPic'] = $tmpPic
Test-Api -Name 'picture/delete 本人删除自己的图片' -Method POST -Path '/picture/delete' -Body @{ id=[long]$tmpPic } -As A -Check { param($d) $d -eq $true }
Test-Api -Name '删除后图片不可见' -Path ('/picture/get/vo?id=' + $tmpPic) -As A -Expect fail

Write-Host '================ S4 空间与团队角色 ================' -ForegroundColor Cyan
Test-Api -Name 'space/get/vo 团队空间' -Path ('/space/get/vo?id=' + $Ctx['teamSpaceId']) -As A -Check { param($d) "$($d.id)" -eq $Ctx['teamSpaceId'] }
Test-Api -Name 'space/edit 本人编辑自己的团队空间' -Method POST -Path '/space/edit' -Body @{ id=[long]$Ctx['teamSpaceId']; spaceName=('e2e团队改名_' + $ts) } -As A -Check { param($d) $d -eq $true }
Test-Api -Name 'space/update 管理员修改空间' -Method POST -Path '/space/update' -Body @{ id=[long]$Ctx['teamSpaceId']; spaceName=('e2e团队_admin_' + $ts) } -As ADMIN -Check { param($d) $d -eq $true }
Test-Api -Name 'spaceUser/add B 为 viewer' -Method POST -Path '/spaceUser/add' -Body @{ spaceId=[long]$Ctx['teamSpaceId']; userId=[long]$Ctx['BId']; spaceRole='viewer' } -As A
Test-Api -Name 'spaceUser/list 团队成员' -Method POST -Path '/spaceUser/list' -Body @{ spaceId=[long]$Ctx['teamSpaceId'] } -As A -Check { param($d) $d.Count -ge 2 }
$bMember = $script:LastData | Where-Object { "$($_.userId)" -eq $Ctx['BId'] } | Select-Object -First 1
$Ctx['bMemberId'] = "$($bMember.id)"
Test-Api -Name 'spaceUser/edit B 升级为 editor' -Method POST -Path '/spaceUser/edit' -Body @{ id=[long]$Ctx['bMemberId']; spaceRole='editor' } -As A -Check { param($d) $d -eq $true }
Test-Api -Name 'editor 可上传团队图片' -Method POST -Path ('/picture/upload?picName=e2e_editor_upload&spaceId=' + $Ctx['teamSpaceId']) -Form @{ file = Get-Item $ImagePath } -As B -Check { param($d) $d.processingStatus -eq 'SUCCEEDED' }
Test-Api -Name 'spaceUser/edit B 降级为 viewer' -Method POST -Path '/spaceUser/edit' -Body @{ id=[long]$Ctx['bMemberId']; spaceRole='viewer' } -As A -Check { param($d) $d -eq $true }
Test-Api -Name 'viewer 上传团队图片应被拒绝' -Method POST -Path ('/picture/upload?picName=e2e_viewer_upload&spaceId=' + $Ctx['teamSpaceId']) -Form @{ file = Get-Item $ImagePath } -As B -Expect fail
Test-Api -Name 'viewer 可查看团队图片' -Path ('/picture/get/vo?id=' + $Ctx['picTeam']) -As B -Check { param($d) "$($d.id)" -eq $Ctx['picTeam'] }
Test-Api -Name '越权: B 修改 A 的团队空间名' -Method POST -Path '/space/update' -Body @{ id=[long]$Ctx['teamSpaceId']; spaceName='hacked' } -As B -Expect fail
Test-Api -Name '越权: C 删除 A 的团队空间' -Method POST -Path '/space/delete' -Body @{ id=[long]$Ctx['teamSpaceId'] } -As C -Expect fail
Test-Api -Name 'space/analyze/usage(owner)' -Method POST -Path '/space/analyze/usage' -Body @{ spaceId=[long]$Ctx['teamSpaceId']; queryAll=$false; queryPublic=$false } -As A
Test-Api -Name 'space/list/page/vo 我的空间' -Method POST -Path '/space/list/page/vo' -Body @{ current=1; pageSize=10 } -As A -Check { param($d) [int]$d.total -ge 2 }

Write-Host '================ S5 访问控制：邀请码 / 配额 / 下载 ================' -ForegroundColor Cyan
Test-Api -Name 'access/quota/today' -Path '/access/quota/today' -As A -Check { param($d) [int]$d.quotaLimit -ge 1 }
$Ctx['quotaBefore'] = [int]$script:LastData.usedCount
Test-Api -Name 'invite/create (viewer)' -Method POST -Path '/access/invite/create' -Body @{ spaceId=[long]$Ctx['teamSpaceId']; spaceRole='viewer'; maxUses=5; expireTime='2027-12-31T00:00:00' } -As A -Check { param($d) $null -ne $d.code }
$Ctx['inviteCode'] = "$($script:LastData.code)"
$Ctx['inviteId'] = "$($script:LastData.id)"
Test-Api -Name 'invite/create 非法角色 admin 应拒绝' -Method POST -Path '/access/invite/create' -Body @{ spaceId=[long]$Ctx['teamSpaceId']; spaceRole='admin'; maxUses=1 } -As A -Expect fail
Test-Api -Name 'invite/list' -Path ('/access/invite/list?spaceId=' + $Ctx['teamSpaceId']) -As A -Check { param($d) $d.Count -ge 1 }
Test-Api -Name 'invite/redeem C 兑换邀请码' -Method POST -Path '/access/invite/redeem' -Body @{ code=$Ctx['inviteCode'] } -As C
Test-Api -Name 'invite/redeem 重复兑换幂等' -Method POST -Path '/access/invite/redeem' -Body @{ code=$Ctx['inviteCode'] } -As C
Test-Api -Name 'invite/revoke 撤销邀请码' -Method POST -Path ('/access/invite/revoke?inviteId=' + $Ctx['inviteId']) -As A

$rid = [guid]::NewGuid().ToString('N')
Test-Api -Name 'download/authorize B 下载公开图' -Method POST -Path '/access/download/authorize' -Body @{ pictureId=[long]$Ctx['picA']; requestId=$rid } -As B -Check { param($d) $null -ne $d.downloadToken }
$Ctx['token'] = "$($script:LastData.downloadToken)"
$Ctx['recordId'] = "$($script:LastData.recordId)"
Test-Api -Name 'download/authorize 同 requestId 幂等' -Method POST -Path '/access/download/authorize' -Body @{ pictureId=[long]$Ctx['picA']; requestId=$rid } -As B -Check { param($d) "$($d.recordId)" -eq $Ctx['recordId'] }
Test-Api -Name 'download/authorize 作者本人下载不扣配额' -Method POST -Path '/access/download/authorize' -Body @{ pictureId=[long]$Ctx['picA']; requestId=([guid]::NewGuid().ToString('N')) } -As A -Check { param($d) [int]$d.quotaCost -eq 0 }
Test-Api -Name 'download/file 令牌换流' -Path ('/access/download/file?token=' + $Ctx['token']) -As B -RawStatus
Test-Api -Name 'download/records 下载审计' -Path '/access/download/records' -As B -Check { param($d) $d.Count -ge 1 }
Test-Api -Name '越权: C 下载 A 私有空间的图片' -Method POST -Path '/access/download/authorize' -Body @{ pictureId=[long]$Ctx['picPrivate']; requestId=([guid]::NewGuid().ToString('N')) } -As C -Expect fail

Write-Host '================ S6 AI 摄影工作台 ================' -ForegroundColor Cyan
Test-Api -Name 'ai/catalog 生成编目' -Method POST -Path ('/ai/catalog?pictureId=' + $Ctx['picA']) -As A -Check { param($d) $d.status -eq 'SUCCEEDED' }
$Ctx['aiTaskId'] = "$($script:LastData.id)"
Test-Api -Name 'ai/meta 查询元数据' -Path ('/ai/meta?pictureId=' + $Ctx['picA']) -As A -Check { param($d) $null -ne $d.subjectTags }
Test-Api -Name 'ai/task/get 任务详情' -Path ('/ai/task/get?id=' + $Ctx['aiTaskId']) -As A -Check { param($d) $d.status -eq 'SUCCEEDED' }
Test-Api -Name 'ai/meta/confirm 人工确认' -Method POST -Path '/ai/meta/confirm' -Body @{ pictureId=[long]$Ctx['picA']; subjectTags='人工确认主体'; sceneTags='人工场景' } -As A -Check { param($d) [int]$d.confirmed -eq 1 }
Test-Api -Name '重新编目不覆盖人工确认字段' -Method POST -Path ('/ai/catalog?pictureId=' + $Ctx['picA']) -As A
Test-Api -Name 'ai/meta 人工字段仍保留' -Path ('/ai/meta?pictureId=' + $Ctx['picA']) -As A -Check { param($d) $d.subjectTags -eq '人工确认主体' -and [int]$d.confirmed -eq 1 }
Test-Api -Name 'ai/search 关键词检索' -Path '/ai/search?query=e2e&topK=10' -As A
Test-Api -Name 'ai/search/semantic 语义检索' -Path '/ai/search/semantic?query=风景&topK=10' -As A
Test-Api -Name 'ai/search/rebuild-index 重建索引' -Method POST -Path '/ai/search/rebuild-index' -As A
Test-Api -Name 'ai/task/exif EXIF 任务' -Method POST -Path ('/ai/task/exif?pictureId=' + $Ctx['picA']) -As A
Test-Api -Name '越权: B 对 A 的图片做编目' -Method POST -Path ('/ai/catalog?pictureId=' + $Ctx['picA']) -As B -Expect fail

Write-Host '================ S7 作品与社区 ================' -ForegroundColor Cyan
Test-Api -Name 'work/save 创建作品(公开)' -Method POST -Path '/work/save' -Body @{ title=('e2e作品_' + $ts); description='端到端测试作品'; visibility='PUBLIC'; downloadPolicy='QUOTA_DOWNLOAD'; pictureIds=@([long]$Ctx['picA']); copyrightNotice='E2E' } -As A -Check { param($d) $null -ne $d }
$Ctx['workId'] = "$($script:LastData)"
Test-Api -Name 'work/publish 发布作品' -Method POST -Path ('/work/publish?workId=' + $Ctx['workId'] + '&publish=true') -As A -Check { param($d) $d -eq $true }
Test-Api -Name 'work/get/vo B 查看作品' -Path ('/work/get/vo?id=' + $Ctx['workId']) -As B -Check { param($d) "$($d.id)" -eq $Ctx['workId'] }
Test-Api -Name 'work/page 公开作品流' -Path '/work/page?current=1&size=10' -As B -Check { param($d) ($d.records | ForEach-Object { "$($_.id)" }) -contains $Ctx['workId'] }
Test-Api -Name '越权: B 发布 A 的作品' -Method POST -Path ('/work/publish?workId=' + $Ctx['workId'] + '&publish=true') -As B -Expect fail
Test-Api -Name 'work/like B 点赞' -Method POST -Path '/work/like' -Body @{ workId=[long]$Ctx['workId'] } -As B -Check { param($d) $d -eq $true }
Test-Api -Name 'work/like 重复点赞幂等(计数=1)' -Method POST -Path '/work/like' -Body @{ workId=[long]$Ctx['workId'] } -As B -Check { param($d) $d -eq $true }
Test-Api -Name 'work/comment B 评论' -Method POST -Path '/work/comment' -Body @{ workId=[long]$Ctx['workId']; content='端到端测试评论' } -As B
Test-Api -Name 'work/comments 评论列表' -Path ('/work/comments?workId=' + $Ctx['workId']) -As B -Check { param($d) $d.Count -ge 1 }
Test-Api -Name 'work/favorite B 收藏' -Method POST -Path '/work/favorite' -Body @{ workId=[long]$Ctx['workId'] } -As B -Check { param($d) $d -eq $true }
Test-Api -Name 'work/follow B 关注 A' -Method POST -Path '/work/follow' -Body @{ userId=[long]$Ctx['AId'] } -As B -Check { param($d) $d -eq $true }
Test-Api -Name 'work/unlike 取消点赞' -Method POST -Path '/work/unlike' -Body @{ workId=[long]$Ctx['workId'] } -As B -Check { param($d) $d -eq $true }
Test-Api -Name 'work/unfavorite 取消收藏' -Method POST -Path '/work/unfavorite' -Body @{ workId=[long]$Ctx['workId'] } -As B -Check { param($d) $d -eq $true }
Test-Api -Name 'work/unfollow 取消关注' -Method POST -Path '/work/unfollow' -Body @{ userId=[long]$Ctx['AId'] } -As B -Check { param($d) $d -eq $true }

Write-Host '================ S8 专题展览 ================' -ForegroundColor Cyan
Test-Api -Name '越权: A 创建展览(非管理员)' -Method POST -Path '/exhibition/add' -Body @{ title='非法展览'; theme='x'; startTime='2026-01-01T00:00:00'; endTime='2027-01-01T00:00:00' } -As A -Expect fail
Test-Api -Name 'exhibition/add 管理员建展' -Method POST -Path '/exhibition/add' -Body @{ title=('e2e专题_' + $ts); theme='城市夜景'; description='E2E'; startTime='2026-01-01T00:00:00'; endTime='2027-12-31T00:00:00'; coverUrl='' } -As ADMIN -Check { param($d) $null -ne $d }
$Ctx['exhId'] = "$($script:LastData)"
Test-Api -Name 'exhibition/work/add 加入已发布作品' -Method POST -Path '/exhibition/work/add' -Body @{ exhibitionId=[long]$Ctx['exhId']; workIds=@([long]$Ctx['workId']) } -As ADMIN
Test-Api -Name 'exhibition/work/list 展品列表(admin)' -Path ('/exhibition/work/list?exhibitionId=' + $Ctx['exhId']) -As ADMIN -Check { param($d) $d.Count -ge 1 }
Test-Api -Name '修复项: 仅含摄影作品的专题可发布' -Method POST -Path '/exhibition/publish' -Body @{ id=[long]$Ctx['exhId'] } -As ADMIN -Check { param($d) $d -eq $true }
Test-Api -Name '修复项: 非管理员浏览已发布专题作品' -Path ('/exhibition/work/list?exhibitionId=' + $Ctx['exhId']) -As B -Check { param($d) $d.Count -ge 1 }
Test-Api -Name 'work/offline 作者下架作品' -Method POST -Path ('/work/offline?workId=' + $Ctx['workId']) -As A -Check { param($d) $d -eq $true }
Test-Api -Name '修复项: 下架作品不再出现在专题中' -Path ('/exhibition/work/list?exhibitionId=' + $Ctx['exhId']) -As B -Check { param($d) $d.Count -eq 0 }
Test-Api -Name '管理员仍可见展品关系' -Path ('/exhibition/work/list?exhibitionId=' + $Ctx['exhId']) -As ADMIN -Check { param($d) $d.Count -ge 1 }
Test-Api -Name '下架作品不在公开作品流' -Path '/work/page?current=1&size=20' -As B -Check { param($d) -not (($d.records | ForEach-Object { "$($_.id)" }) -contains $Ctx['workId']) }
Test-Api -Name 'exhibition/publish 重复发布(幂等)' -Method POST -Path '/exhibition/publish' -Body @{ id=[long]$Ctx['exhId'] } -As ADMIN
Test-Api -Name 'exhibition/picture/add 加入图片展品' -Method POST -Path '/exhibition/picture/add' -Body @{ exhibitionId=[long]$Ctx['exhId']; pictureIds=@([long]$Ctx['picA']) } -As ADMIN
Test-Api -Name 'exhibition/publish 发布专题' -Method POST -Path '/exhibition/publish' -Body @{ id=[long]$Ctx['exhId'] } -As ADMIN
Test-Api -Name 'exhibition/list/page 公开列表' -Method POST -Path '/exhibition/list/page' -Body @{ current=1; pageSize=10 } -As B -Check { param($d) ($d.records | ForEach-Object { "$($_.id)" }) -contains $Ctx['exhId'] }
Test-Api -Name 'exhibition/get/vo 详情' -Path ('/exhibition/get/vo?id=' + $Ctx['exhId']) -As B -Check { param($d) "$($d.id)" -eq $Ctx['exhId'] }
Test-Api -Name 'exhibition/work/remove 移除展品' -Method POST -Path '/exhibition/work/remove' -Body @{ exhibitionId=[long]$Ctx['exhId']; workIds=@([long]$Ctx['workId']) } -As ADMIN
Test-Api -Name 'exhibition/offline 下架专题' -Method POST -Path '/exhibition/offline' -Body @{ id=[long]$Ctx['exhId'] } -As ADMIN

Write-Host '================ S9 其它模块 ================' -ForegroundColor Cyan
Test-Api -Name 'picture/tag_category 标签分类' -Path '/picture/tag_category' -As A
Test-Api -Name 'history/record 记录浏览' -Method POST -Path ('/history/record?pictureId=' + $Ctx['picA']) -As B
Test-Api -Name 'history/list/my 我的浏览' -Path '/history/list/my?current=1&size=10' -As B
Test-Api -Name 'collection/add 创建图集' -Method POST -Path '/collection/add' -Body @{ name=('e2e图集_' + $ts); description='E2E'; isPublic=0 } -As A
$Ctx['collId'] = "$($script:LastData)"
Test-Api -Name 'collection/list/my 我的图集' -Path '/collection/list/my' -As A -Check { param($d) $d.Count -ge 1 }
Test-Api -Name 'user/list/page/vo 管理员用户列表' -Method POST -Path '/user/list/page/vo' -Body @{ current=1; pageSize=5 } -As ADMIN -Check { param($d) [int]$d.total -ge 4 }
Test-Api -Name 'log/list/page 操作日志(admin)' -Method POST -Path '/log/list/page' -Body @{ current=1; pageSize=5 } -As ADMIN
Test-Api -Name '越权: B 查看操作日志' -Method POST -Path '/log/list/page' -Body @{ current=1; pageSize=5 } -As B -Expect fail

Write-Host ''
Write-Host '================ S10 游客访问与演示数据恢复 ================' -ForegroundColor Cyan
Test-Api -Name '恢复: 作者重新发布作品' -Method POST -Path ('/work/publish?workId=' + $Ctx['workId'] + '&publish=true') -As A -Check { param($d) $d -eq $true }
Test-Api -Name '恢复: 管理员重新发布专题' -Method POST -Path '/exhibition/publish' -Body @{ id=[long]$Ctx['exhId'] } -As ADMIN -Check { param($d) $d -eq $true }
Test-Api -Name '恢复: 专题重新加入作品' -Method POST -Path '/exhibition/work/add' -Body @{ exhibitionId=[long]$Ctx['exhId']; workIds=@([long]$Ctx['workId']) } -As ADMIN
Test-Api -Name '游客: 公共图库列表可访问' -Method POST -Path '/picture/list/page/vo' -Body @{ current=1; pageSize=5 } -As NONE -Check { param($d) [int]$d.total -ge 1 }
Test-Api -Name '游客: 公开作品流可访问' -Path '/work/page?current=1&size=5' -As NONE -Check { param($d) [int]$d.total -ge 1 }
Test-Api -Name '游客: 公开作品详情可访问' -Path ('/work/get/vo?id=' + $Ctx['workId']) -As NONE -Check { param($d) "$($d.id)" -eq $Ctx['workId'] }
Test-Api -Name '游客: 公开作品详情返回正确作品' -Path ('/work/get/vo?id=' + $Ctx['workId']) -As NONE -Check { param($d) $d.visibility -eq 'PUBLIC' }
Test-Api -Name '私有作品(游客不可见)' -Method POST -Path '/work/save' -Body @{ title='游客不可见作品'; visibility='PRIVATE'; downloadPolicy='OWNER_ONLY'; pictureIds=@([long]$Ctx['picA']) } -As A
$Ctx['privateWorkId'] = "$($script:LastData)"
Test-Api -Name '游客: 私有作品详情被拒绝' -Path ('/work/get/vo?id=' + $Ctx['privateWorkId']) -As NONE -Expect fail
Test-Api -Name '游客: 专题列表可访问' -Method POST -Path '/exhibition/list/page' -Body @{ current=1; pageSize=5 } -As NONE -Check { param($d) ($d.records | ForEach-Object { "$($_.id)" }) -contains $Ctx['exhId'] }
Test-Api -Name '游客: 专题详情可访问' -Path ('/exhibition/get/vo?id=' + $Ctx['exhId']) -As NONE -Check { param($d) "$($d.id)" -eq $Ctx['exhId'] }
Test-Api -Name '游客: 专题作品列表可访问' -Path ('/exhibition/work/list?exhibitionId=' + $Ctx['exhId']) -As NONE -Check { param($d) $d.Count -ge 1 }
Test-Api -Name '管理员下架专题' -Method POST -Path '/exhibition/offline' -Body @{ id=[long]$Ctx['exhId'] } -As ADMIN -Check { param($d) $d -eq $true }
Test-Api -Name '游客: 下架专题详情被拒绝' -Path ('/exhibition/get/vo?id=' + $Ctx['exhId']) -As NONE -Expect fail
Test-Api -Name '最终恢复: 专题重新发布(演示数据)' -Method POST -Path '/exhibition/publish' -Body @{ id=[long]$Ctx['exhId'] } -As ADMIN -Check { param($d) $d -eq $true }

Write-Host ''
Write-Host '================ S17 AI 可靠性（迭代 1） ================' -ForegroundColor Cyan
Test-Api -Name 'S17 上传带 EXIF 的样张' -Method POST -Path '/picture/upload?picName=e2e_exif_sample' -Form @{ file = Get-Item $ExifImagePath } -As A -Check { param($d) $d.processingStatus -eq 'SUCCEEDED' }
$Ctx['exifPic'] = "$($script:LastData.id)"
Test-Api -Name 'S17 上传阶段解析到真实 EXIF(品牌/型号/时间)' -Path ('/ai/meta?pictureId=' + $Ctx['exifPic']) -As A -Check { param($d) $d.cameraMake -eq 'E2E-Camera-Co' -and $d.cameraModel -eq 'E2E-X100' -and $null -ne $d.shootTime }
Test-Api -Name 'S17 EXIF 含镜头/光圈/焦距/ISO' -Path ('/ai/meta?pictureId=' + $Ctx['exifPic']) -As A -Check { param($d) $d.lensModel -eq 'E2E 35mm f/1.8' -and $d.exifJson -match 'fNumber' -and $d.exifJson -match 'focalLength' -and $d.exifJson -match 'exposureTime' -and $d.exifJson -match 'iso' }
Test-Api -Name 'S17 EXIF 任务可执行（存储剥离时降级）' -Method POST -Path ('/ai/task/exif?pictureId=' + $Ctx['exifPic']) -As A -Check { param($d) $d.status -eq 'SUCCEEDED' }
Test-Api -Name 'S17 我的任务列表含新任务' -Path '/ai/task/page/my?current=1&size=20' -As A -Check { param($d) $d.Count -ge 1 }
$Ctx['catalogTaskId'] = "$($script:LastData[0].id)"
Test-Api -Name 'S17 已成功任务不可重试' -Method POST -Path ('/ai/task/retry?id=' + $Ctx['catalogTaskId']) -As A -Expect fail
Test-Api -Name 'S17 已成功任务不可取消' -Method POST -Path ('/ai/task/cancel?id=' + $Ctx['catalogTaskId']) -As A -Expect fail
Run-Sql ("update photo_ai_task set status='FAILED', errorMessage='e2e 构造失败' where id=" + $Ctx['catalogTaskId']) | Out-Null
Test-Api -Name 'S17 失败任务可重试并成功' -Method POST -Path ('/ai/task/retry?id=' + $Ctx['catalogTaskId']) -As A -Check { param($d) $d.status -eq 'SUCCEEDED' -and [int]$d.retryCount -eq 1 }
Test-Api -Name 'S17 重试后状态可查询' -Path ('/ai/task/get?id=' + $Ctx['catalogTaskId']) -As A -Check { param($d) $d.status -eq 'SUCCEEDED' }
Test-Api -Name 'S17 相似分组：上传第二张同规格样张' -Method POST -Path '/picture/upload?picName=e2e_exif_pair' -Form @{ file = Get-Item $ExifImagePath } -As A -Check { param($d) $d.processingStatus -eq 'SUCCEEDED' }
Test-Api -Name 'S17 相似分组返回分组与推荐保留' -Path ('/ai/similar/group?pictureId=' + $Ctx['exifPic']) -As A -Check { param($d) $d.groups.Count -ge 1 -and $null -ne $d.groups[0].recommendPictureId }
Test-Api -Name 'S17 相似分组不删除原图' -Path ('/picture/get/vo?id=' + $Ctx['exifPic']) -As A -Check { param($d) "$($d.id)" -eq $Ctx['exifPic'] }
Test-Api -Name 'S17 检索返回命中字段与标签' -Path '/ai/search/evidence?query=%E9%A3%8E%E6%99%AF&topK=5' -As A -Check { param($d) $d.Count -ge 1 -and $null -ne $d[0].matchedFields }
Test-Api -Name 'S17 检索结果带权限依据' -Path '/ai/search/evidence?query=%E9%A3%8E%E6%99%AF&topK=5' -As A -Check { param($d) $d[0].accessReason -match '本人|团队|审核' }
Test-Api -Name 'S17 他人任务列表相互隔离' -Path '/ai/task/page/my' -As B -Check { param($d) $d.Count -eq 0 }


# ================= S18 通知中心 =================
$Current = 'S18 通知中心'
Test-Api -Name 'S18 未读数可查询' -Path '/notification/unread/count' -As A -Check { param($d) $null -ne $d.count }
Test-Api -Name 'S18 通知列表可查询' -Path '/notification/list?current=1&size=10' -As A -Check { param($d) $null -ne $d.total }
Test-Api -Name 'S18 通知仅本人可见' -Path '/notification/list?current=1&size=10' -As C -Check { param($d) ($d.records | Where-Object { "$($_.userId)" -eq $Ctx['aId'] }).Count -eq 0 }
Test-Api -Name 'S18 互动触发通知（C 点赞 A 的作品）' -Method POST -Path '/work/like' -Body @{ workId = $Ctx['workId'] } -As C -Expect ok
Test-Api -Name 'S18 点赞后 A 有未读通知' -Path '/notification/unread/count' -As A -Check { param($d) [int]$d.count -ge 1 }
Test-Api -Name 'S18 标记全部已读' -Method POST -Path '/notification/read' -Body @{ all = $true } -As A -Check { param($d) [int]$d -ge 0 }
Test-Api -Name 'S18 已读后未读数归零' -Path '/notification/unread/count' -As A -Check { param($d) [int]$d.count -eq 0 }
Test-Api -Name 'S18 未登录不可查通知' -Path '/notification/list' -As NONE -Expect fail

# ================= S19 内容举报 =================
$Current = 'S19 内容举报'
Test-Api -Name 'S19 普通用户不可访问举报中心' -Path '/admin/console/reports' -As A -Expect fail
Test-Api -Name 'S19 提交举报' -Method POST -Path '/report/submit' -Body @{ targetType='work'; targetId=$Ctx['workId']; reason='内容违规'; detail='e2e 举报' } -As C -Check { param($d) $null -ne $d }
$Ctx['reportId'] = "$($script:LastData)"
Test-Api -Name 'S19 重复举报被拦截' -Method POST -Path '/report/submit' -Body @{ targetType='work'; targetId=$Ctx['workId']; reason='重复举报' } -As C -Expect fail
Test-Api -Name 'S19 管理员可见待处理举报' -Path '/admin/console/reports?status=PENDING' -As ADMIN -Check { param($d) [int]$d.pendingCount -ge 1 }
Test-Api -Name 'S19 管理员处置举报' -Method POST -Path '/admin/console/reports/handle' -Body @{ id=$Ctx['reportId']; action='RESOLVE'; result='e2e 已核实' } -As ADMIN -Check { param($d) $d -eq $true }
Test-Api -Name 'S19 举报人收到结果通知' -Path '/notification/list?current=1&size=10' -As C -Check { param($d) ($d.records | Where-Object { $_.title -like '*举报*' }).Count -ge 1 }

# ================= S20 审核申诉 =================
$Current = 'S20 审核申诉'
Test-Api -Name 'S20 管理员拒绝 A 的图片' -Method POST -Path '/admin/console/moderate' -Body @{ type='picture'; action='REJECT'; ids=@($Ctx['picA']); reason='e2e 驳回' } -As ADMIN -Check { param($d) [int]$d -ge 1 }
Test-Api -Name 'S20 非作者不可申诉' -Method POST -Path '/appeal/submit' -Body @{ pictureId=$Ctx['picA']; reason='越权申诉' } -As B -Expect fail
Test-Api -Name 'S20 作者提交申诉' -Method POST -Path '/appeal/submit' -Body @{ pictureId=$Ctx['picA']; reason='e2e 申诉理由' } -As A -Check { param($d) $null -ne $d }
$Ctx['appealId'] = "$($script:LastData)"
Test-Api -Name 'S20 重复申诉被拦截' -Method POST -Path '/appeal/submit' -Body @{ pictureId=$Ctx['picA']; reason='重复申诉' } -As A -Expect fail
Test-Api -Name 'S20 管理员可见待处理申诉' -Path '/admin/console/appeals?status=PENDING' -As ADMIN -Check { param($d) [int]$d.pendingCount -ge 1 }
Test-Api -Name 'S20 受理申诉' -Method POST -Path '/admin/console/appeals/handle' -Body @{ id=$Ctx['appealId']; action='ACCEPT'; result='e2e 复核通过' } -As ADMIN -Check { param($d) $d -eq $true }
Test-Api -Name 'S20 受理后图片重新上架' -Path ('/picture/get/vo?id=' + $Ctx['picA']) -As A -Check { param($d) [int]$d.reviewStatus -eq 1 }
Test-Api -Name 'S20 作者收到申诉结果通知' -Path '/notification/list?current=1&size=10' -As A -Check { param($d) ($d.records | Where-Object { $_.title -like '*申诉*' }).Count -ge 1 }

# ================= S21 机审（自动审核） =================
$Current = 'S21 机审'
Test-Api -Name 'S21 普通用户不可读机审策略' -Path '/admin/console/auto-review/config' -As A -Expect fail
Test-Api -Name 'S21 管理员读取机审策略' -Path '/admin/console/auto-review/config' -As ADMIN -Check { param($d) $null -ne $d.'autoReview.enabled' }
Test-Api -Name 'S21 保存机审策略' -Method POST -Path '/admin/console/auto-review/config' -Body @{ 'autoReview.minWidth'=300 } -As ADMIN -Check { param($d) $d -eq $true }
Test-Api -Name 'S21 一键机审可运行' -Method POST -Path '/admin/console/auto-review/run?limit=50' -As ADMIN -Check { param($d) $null -ne $d.scanned }
Test-Api -Name 'S21 发布带风险词的作品' -Method POST -Path '/work/save' -Body @{ title='赌博推广e2e'; description='e2e 机审'; visibility='PUBLIC'; pictureIds=@($Ctx['picA']) } -As A -Check { param($d) $null -ne $d }
$Ctx['riskWorkId'] = "$($script:LastData)"
Test-Api -Name 'S21 发布该作品' -Method POST -Path ('/work/publish?workId=' + $Ctx['riskWorkId'] + '&publish=true') -As A -Expect ok
Test-Api -Name 'S21 风险作品进入待审' -Path ('/work/get/vo?id=' + $Ctx['riskWorkId']) -As A -Check { param($d) [int]$d.reviewStatus -eq 0 }
Test-Api -Name 'S21 待审作品不出现在公开流' -Path '/work/page?current=1&size=50' -As NONE -Check { param($d) ($d.records | ForEach-Object { "$($_.id)" }) -notcontains $Ctx['riskWorkId'] }

# ================= S22 评论点赞与评论机审 =================
$Current = 'S22 评论点赞'
Test-Api -Name 'S22 发表正常评论' -Method POST -Path '/work/comment' -Body @{ workId=$Ctx['workId']; content='e2e 正常评论' } -As C -Check { param($d) $null -ne $d.id }
$Ctx['commentId'] = "$($script:LastData.id)"
Test-Api -Name 'S22 评论初始点赞数为 0' -Path ('/work/comments?workId=' + $Ctx['workId']) -As C -Check { param($d) [int](($d | Where-Object { "$($_.id)" -eq $Ctx['commentId'] }).likeCount) -eq 0 }
Test-Api -Name 'S22 点赞评论' -Method POST -Path '/work/comment/like' -Body @{ workId=$Ctx['commentId'] } -As B -Check { param($d) $d -eq $true }
Test-Api -Name 'S22 点赞数变为 1' -Path ('/work/comments?workId=' + $Ctx['workId']) -As B -Check { param($d) [int](($d | Where-Object { "$($_.id)" -eq $Ctx['commentId'] }).likeCount) -eq 1 }
Test-Api -Name 'S22 取消点赞' -Method POST -Path '/work/comment/unlike' -Body @{ workId=$Ctx['commentId'] } -As B -Check { param($d) $d -eq $true }
Test-Api -Name 'S22 点赞数回到 0' -Path ('/work/comments?workId=' + $Ctx['workId']) -As B -Check { param($d) [int](($d | Where-Object { "$($_.id)" -eq $Ctx['commentId'] }).likeCount) -eq 0 }
Test-Api -Name 'S22 评论含风险词' -Method POST -Path '/work/comment' -Body @{ workId=$Ctx['workId']; content='这里有赌博链接' } -As C -Expect ok
Test-Api -Name 'S22 管理员可见已隐藏评论' -Path '/admin/console/comments?status=HIDDEN' -As ADMIN -Check { param($d) [int]$d.hiddenCount -ge 1 }
Test-Api -Name 'S22 普通用户不可访问评论管理' -Path '/admin/console/comments' -As C -Expect fail

# ================= S23 关注流与相关推荐 =================
$Current = 'S23 关注流'
Test-Api -Name 'S23 关注流可查询' -Path '/work/page?current=1&size=5&feed=following' -As B -Check { param($d) $null -ne $d.total }
Test-Api -Name 'S23 相关推荐返回作品' -Path ('/work/related?id=' + $Ctx['workId'] + '&limit=6') -As A -Check { param($d) $d.Count -ge 1 }
Test-Api -Name 'S23 相关推荐不含自身' -Path ('/work/related?id=' + $Ctx['workId'] + '&limit=6') -As A -Check { param($d) ($d | ForEach-Object { "$($_.id)" }) -notcontains $Ctx['workId'] }
Test-Api -Name 'S23 详情返回浏览量' -Path ('/work/get/vo?id=' + $Ctx['workId']) -As NONE -Check { param($d) $null -ne $d.viewCount }

# ================= S24 成就与审核流水 =================
$Current = 'S24 成就与流水'
Test-Api -Name 'S24 查看自己的成就' -Path '/user/achievement' -As A -Check { param($d) [int]$d.level -ge 1 -and $d.badges.Count -ge 5 }
Test-Api -Name 'S24 查看他人成就' -Path ('/user/achievement?userId=' + $Ctx['aId']) -As B -Check { param($d) $null -ne $d.levelName }
Test-Api -Name 'S24 普通用户不可查审核流水' -Path '/admin/console/review-logs' -As A -Expect fail
Test-Api -Name 'S24 管理员可查审核流水' -Path '/admin/console/review-logs?current=1&size=10' -As ADMIN -Check { param($d) [int]$d.total -ge 1 }
Test-Api -Name 'S24 流水含动作与操作人' -Path '/admin/console/review-logs?current=1&size=5' -As ADMIN -Check { param($d) $null -ne $d.records[0].action -and $null -ne $d.records[0].operatorName }


# ================= S25 专题详情（B1）================
$Current = 'S25 专题详情'
Test-Api -Name 'S25 专题详情返回专题与展品' -Path ('/exhibition/detail?exhibitionId=' + $Ctx['exhId']) -As B -Check { param($d) "$($d.exhibition.id)" -eq $Ctx['exhId'] -and $null -ne $d.works }
Test-Api -Name 'S25 专题详情含浏览计数' -Path ('/exhibition/detail?exhibitionId=' + $Ctx['exhId']) -As B -Check { param($d) [int]$d.exhibition.viewCount -ge 1 }
Test-Api -Name 'S25 专题详情返回展品说明映射' -Path ('/exhibition/detail?exhibitionId=' + $Ctx['exhId']) -As B -Check { param($d) $null -ne $d.captions }
Test-Api -Name 'S25 管理员新建未发布专题' -Method POST -Path '/exhibition/add' -Body @{ title=('e2e未发布专题_' + $ts); theme='未发布'; description='E2E'; startTime='2026-01-01T00:00:00'; endTime='2027-01-01T00:00:00' } -As ADMIN -Check { param($d) $null -ne $d }
$Ctx['draftExhId'] = "$($script:LastData)"
Test-Api -Name 'S25 未发布专题：普通用户被拒' -Path ('/exhibition/detail?exhibitionId=' + $Ctx['draftExhId']) -As B -Expect fail
Test-Api -Name 'S25 未发布专题：管理员可见' -Path ('/exhibition/detail?exhibitionId=' + $Ctx['draftExhId']) -As ADMIN -Check { param($d) $null -ne $d.exhibition }
Test-Api -Name 'S25 不存在的专题返回错误' -Path '/exhibition/detail?exhibitionId=99999999999' -As B -Expect fail

Write-Host '================ 汇总 ================' -ForegroundColor Cyan
$pass = ($script:Results | Where-Object { $_.pass }).Count
$fail = ($script:Results | Where-Object { -not $_.pass }).Count
$total = $script:Results.Count
Write-Host ('总计 ' + $total + ' 项：PASS ' + $pass + ' / FAIL ' + $fail)
Write-Host ''
Write-Host '失败清单：'
$script:Results | Where-Object { -not $_.pass } | ForEach-Object { Write-Host ('  - ' + $_.name + ' -> code=' + $_.code + ' msg=' + $_.message) -ForegroundColor Red }

$report = [pscustomobject]@{
  timestamp = (Get-Date).ToString('s')
  base = $Base
  total = $total
  pass = $pass
  fail = $fail
  accounts = @{ A=$acctA; B=$acctB; C=$acctC; ADMIN=$acctAdmin }
  context = $Ctx
  results = $script:Results
}
$jsonPath = Join-Path $OutDir ('e2e-report-' + $ts + '.json')
$report | ConvertTo-Json -Depth 8 | Set-Content -Path $jsonPath -Encoding UTF8
Write-Host ('报告已写入: ' + $jsonPath)
exit $fail
