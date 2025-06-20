f = ReadFile("options.txt")
screenwidth = GetINIInt("options.ini","options","width")
screenheight = GetINIInt("options.ini","options","height")
colordepth = GetINIInt("options.ini","options","colordepth")
fullscreen = GetINIInt("options.ini","options","fullscreen")

Global InvertMouse =  GetINIInt("options.ini","options","invert mouse y")

If fullscreen Then
	Graphics3D (screenwidth,screenheight,colordepth, 1)
Else
	Graphics3D (screenwidth,screenheight,colordepth, 2)
EndIf

AppTitle "SCP-087-B [Remix created by Steve Monke]"
	
AntiAlias True 
HidePointer 
SetBuffer BackBuffer()

Global font1=LoadFont( "GFX\pretext.ttf",18 ) 
Global font=LoadFont( "GFX\Courier.ttf",128 ) 

Const hit_map = 1
Const hit_player = 2

SeedRnd MilliSecs()

; -- Viewport.
Global viewport_center_x = GraphicsWidth () / 2
Global viewport_center_y = GraphicsHeight () / 2
;^^^^^^

; -- Mouselook.
Global mouselook_x_inc# = 0.3 ; This sets both the sensitivity and direction (+/-) of the mouse on the X axis.
Global mouselook_y_inc# = 0.3 ; This sets both the sensitivity and direction (+/-) of the mouse on the Y axis.
Global mouse_left_limit = 250 ; Used to limit the mouse movement to within a certain number of pixels (250 is used here) from the center of the screen. This produces smoother mouse movement than continuously moving the mouse back to the center each loop.
Global mouse_right_limit = GraphicsWidth () - 250 ; As above.
Global mouse_top_limit = 250 ; As above.
Global mouse_bottom_limit = GraphicsHeight () - 250 ; As above.
;^^^^^^

; -- Mouse smoothing que.
Global mouse_x_speed_1#, mouse_x_speed_2#,mouse_x_speed_3#,mouse_x_speed_4#,mouse_x_speed_5#
Global mouse_y_speed_1#,mouse_y_speed_2#,mouse_y_speed_3#,mouse_y_speed_4#,mouse_y_speed_5#

; -- User.
Global user_camera_pitch#
Global up#, side#
;^^^^^^

Global flooramount = 210
Dim FloorActions(flooramount)
Dim FloorTimer(flooramount)

Type ENEMIES
	Field obj 		; objekti
	Field collider	; "törmäyspallo"
	Field yspeed#	;pudotusnopeus
	Field speed#
	Field oldX 
	Field oldZ
End Type

Type GLIMPSES
	Field obj
End Type 

Global camera
camera = CreateCamera()
CameraRange camera, 0.001, 100
CameraFogMode camera, 1
CameraFogRange camera, 1, 2.4
CameraFogColor camera,0,0,0
microphone=CreateListener(camera) ; Create listener, make it child of camera

Global BlurTimer, Brightness=max(min(GetINIInt("options.ini","options","brightness"),255),0)
Include "dreamfilter.bb"
CreateBlurImage()

Global PlayerFloor, KillTimer%

AmbientLight Brightness,Brightness,Brightness

Global collider = CreatePivot()
PositionEntity collider,-2.5,-1.3,-0.5
EntityType collider, hit_player
EntityRadius collider, 0.3

Dim FloorNumberTexture(flooramount+1)
Global brickwalltexture = LoadTexture("GFX\brickwall.jpg")
Global sign = LoadImage("GFX\sign.jpg")
Global map, map1, map2, map3


Global isCrouch = 0


Global CurrEnemy.ENEMIES, CurrObject, SoundEmitter = CreatePivot()
ScaleEntity SoundEmitter, 0.1,0.1,0.1

Const ACT_STEPS = 1, ACT_LIGHTS = 2, ACT_FLASH = 3, ACT_WALK = 4, ACT_RUN = 5, ACT_KALLE = 6, ACT_BREATH = 7
Const ACT_PROCEED = 8, ACT_TRAP=9, ACT_173 = 11, ACT_CELL = 12, ACT_LOCK = 13
Const ACT_RADIO2 = 15, ACT_RADIO3 = 16, ACT_RADIO4 = 17, ACT_TRICK1 = 18, ACT_TRICK2 = 19
Const ACT_ROAR = 20, ACT_DARKNESS = 21

Collisions hit_player,hit_map, 2,3
		
Global stepsound=Load3DSound("SFX\step.wav") ; Load 3D sound
Global loudstepsound=Load3DSound("SFX\loudstep.ogg") ; Load 3D sound
Dim HorrorSFX(3)
For i = 0 To 2
	HorrorSFX(i)=LoadSound("SFX\horror"+(i+1)+".ogg")
Next
Global DeathSFX = LoadSound("SFX\death.ogg")
Global RoarSFX = LoadSound("SFX\roar.ogg")
Global BreathSFX = LoadSound("SFX\breath.ogg")
Global StoneSFX = LoadSound("SFX\stone.ogg")
Global NoSFX = LoadSound("SFX\no.ogg")


Dim AmbientSFX(10)
For i = 0 To 8
	AmbientSFX(i)=Load3DSound("SFX\ambient"+(i+1)+".ogg")
Next

Global DontlookSFX = LoadSound("SFX\dontlook.ogg")
Dim RadioSFX(5)
For i = 0 To 3
	RadioSFX(i)=LoadSound("SFX\radio"+(i+1)+".ogg")
Next

Global Music = LoadSound("SFX\music.ogg"), MusicChannel

Global FireOn = LoadSound("SFX\match.ogg")
Global FireOff = LoadSound("SFX\fireout.ogg")

Global mentalmesh = LoadAnimMesh("GFX\mental.b3d")
Global mental = LoadTexture("GFX\mental.jpg")
Global tex173 = LoadTexture("GFX\173.jpg")

Dim GlimpseTextures(5)
GlimpseTextures(0)= LoadTexture("GFX\glimpse1.png",1+2)
GlimpseTextures(1)= LoadTexture("GFX\glimpse2.png",1+2)

Collisions hit_player,hit_player, 1,3
Collisions hit_player,hit_map, 2,2 

CreateMap(flooramount)
CreateGlimpses()

framelimit = 300

MusicChannel = PlaySound(Music)

;-------------------------------------------------------------------------------
;-------------------------------------------------------------------------------

Color 255,255,255
While Not KeyDown(1)


	If FrameLimit > 0 Or FrameLimit < 255
	   	; Framelimit
		WaitingTime = (1000.0 / FrameLimit) - (MilliSecs() - LoopDelay)
		Delay WaitingTime
	   LoopDelay = MilliSecs()
	EndIf 

	elapsedloops = elapsedloops + 1
	If timer < MilliSecs() Then
		loops = elapsedloops	
		elapsedloops = 0
		timer = MilliSecs() + 1000 
	EndIf 
	
	If Not ChannelPlaying (MusicChannel) Then MusicChannel = PlaySound(Music)
	
; Crouch toggle (hold Ctrl: 29 = Left Ctrl, 157 = Right Ctrl)
crouching = (KeyDown(29) Or KeyDown(157))

If crouching Then
	isCrouch = 1
Else
	isCrouch = 0
EndIf

; Adjust speed and bob multiplier
sprintMultiplier# = 1.0
If KeyDown(42) Or KeyDown(54) Then 

	sprintMultiplier# = 2.0

EndIf

; Adjust speed and bob multiplier
sprintMultiplier# = 1.0
If KeyDown(42) Or KeyDown(54) Then sprintMultiplier# = 2.0
If crouching Then sprintMultiplier# = 0.5

; Vertical adjustment for crouching - world position based
If crouching Then
    PlayerFloor = (-EntityY(collider) - 0.25) / 4
    
    ; Calculate world crouch offset based on player Y position or world coordinates
    crouchOffsetWorld# = EntityY(collider) - 0.5  ; offset crouch by lowering camera in world Y
    
Else
    PlayerFloor = (-EntityY(collider) - 0.5) / 2
    crouchOffsetWorld# = EntityY(collider)  ; no offset, normal standing height
EndIf

; Movement input
If KeyDown(208) Or KeyDown(31) Then MoveEntity collider, 0, 0, -0.015 * sprintMultiplier#
If KeyDown(200) Or KeyDown(17) Then MoveEntity collider, 0, 0,  0.02  * sprintMultiplier#
If KeyDown(203) Or KeyDown(30) Then MoveEntity collider, -0.008 * sprintMultiplier#, 0, 0
If KeyDown(205) Or KeyDown(32) Then MoveEntity collider,  0.008 * sprintMultiplier#, 0, 0

; Movement detection
moving = ((KeyDown(208) Or KeyDown(31)) Xor KeyDown(200) Or KeyDown(17)) Or ((KeyDown(203) Or KeyDown(30)) Xor (KeyDown(205) Or KeyDown(32)))

; Then use crouchOffsetWorld# for vertical offset in bobbing
If moving Then
    bobTime# = bobTime# + 4 * sprintMultiplier#

    shake#  = Sin(bobTime# * 1.5) * 0.08
    shakeX# = Sin(bobTime# * 0.75) * 0.05

    ; Integrate crouchOffsetWorld# into vertical offset 'up' scaled properly
    up   = up * 0.9 + (shake# + (crouchOffsetWorld# - EntityY(collider))) * 0.1
    side = side * 0.9 + shakeX# * 0.1
    roll = roll * 0.9 + shakeX# * 0.03

    If Abs(Sin(bobTime# * 1)) < 0.05 Then EmitSound(stepsound, collider)
Else
    bobTime# = 0
    up   = up * 0.95 + (crouchOffsetWorld# - EntityY(collider)) * 0.05
    side = side * 0.95
    roll = roll * 0.95
    lastStepTime# = 0
EndIf

	
	UpdateEnemies()
		
	CollidedFloor = False
	For i = 1 To CountCollisions(collider)
		If CollisionY(collider,i) < EntityY(collider) - 0.1 Then collidedFloor = True 
	Next
	
	If Rand(1000) < 2 Then
		PositionEntity(SoundEmitter, EntityX(collider)+Rand(-1,1), EntityY(collider)+Rand(-2,-10), EntityZ(collider)+Rand(-1,1))
		EmitSound(AmbientSFX(Rand(0,8)),SoundEmitter) 
	EndIf
		
	If CollidedFloor = True Then
		If dropspeed# < -0.09 Then KillTimer = max(1,KillTimer)
		dropspeed# = 0
	Else
		dropspeed# = dropspeed-0.004
		MoveEntity collider,0,dropspeed,0			
	EndIf
	
	UpdateFloors()
	UpdateGlimpses()
	
	If KillTimer>0 Then Kill()
	
	UpdateWorld 
	RenderWorld

	BlurTimer = max(BlurTimer-1,0)
		If KillTimer > 0 Then
		UpdateBlur(0.93)	
	Else
		If BlurTimer < 50 Then 
			UpdateBlur(0.7+(BlurTimer/50.0)*0.2)
		Else
			UpdateBlur(0.7+0.2)	
		EndIf		
	EndIf
	
	MouseLook()
	
	Flip
	
Wend 

;---------------------------------------------------------------------------------
;---------------------------------------------------------------------------------

Function min#(n1#, n2#)
	If n1 < n2 Then Return n1 Else Return n2
End Function

Function max#(n1#, n2#)
	If n1 > n2 Then Return n1 Else Return n2
End Function

Function CurveValue#(number#, oldn#, smooth#)
    Return oldn + (number - oldn) * (1.0 / smooth)
End Function

Function MouseLook()
    ; Position the camera at the player position with appropriate offsets
    PositionEntity camera, EntityX(collider), EntityY(collider)+1, EntityZ(collider)
    MoveEntity camera, side, up, 0
    
    ; -- Update the smoothing queue to smooth the movement of the mouse.
    mouse_x_speed_5# = mouse_x_speed_4#
    mouse_x_speed_4# = mouse_x_speed_3#
    mouse_x_speed_3# = mouse_x_speed_2#
    mouse_x_speed_2# = mouse_x_speed_1#
    mouse_x_speed_1# = MouseXSpeed()
    mouse_y_speed_5# = mouse_y_speed_4#
    mouse_y_speed_4# = mouse_y_speed_3#
    mouse_y_speed_3# = mouse_y_speed_2#
    mouse_y_speed_2# = mouse_y_speed_1#
    
    If InvertMouse Then
        mouse_y_speed_1# = -MouseYSpeed()
    Else
        mouse_y_speed_1# = MouseYSpeed()
    EndIf
    
    ; Calculate standard mouselook rotation
    the_yaw# = ((mouse_x_speed_1# + mouse_x_speed_2# + mouse_x_speed_3# + mouse_x_speed_4# + mouse_x_speed_5#) / 5.0) * mouselook_x_inc#
    the_pitch# = ((mouse_y_speed_1# + mouse_y_speed_2# + mouse_y_speed_3# + mouse_y_speed_4# + mouse_y_speed_5#) / 5.0) * mouselook_y_inc#
    
    ; Get relative mouse position for camera aiming effect
    mouseRelX# = (MouseX() - viewport_center_x) / 100.0
    mouseRelY# = (MouseY() - viewport_center_y) / 100.0
    
    ; Apply a subtle aim offset based on mouse cursor position
    aim_offset_yaw# = mouseRelX# * 2.0
    aim_offset_pitch# = mouseRelY# * 2.0
    
    ; Turn the player entity
    TurnEntity collider, 0.0, -the_yaw#, 0.0
    user_camera_pitch# = user_camera_pitch# + the_pitch#
    
    ; Limit the pitch to reasonable angles
    If user_camera_pitch# > 70.0 Then user_camera_pitch# = 70.0
    If user_camera_pitch# < -70.0 Then user_camera_pitch# = -70.0
    
    ; Apply both the standard mouselook rotation and the subtle aim-at-cursor effect
    RotateEntity camera, user_camera_pitch# + aim_offset_pitch#, EntityYaw(collider) + aim_offset_yaw#, roll
    
    ; Reset mouse position if it moves too far from center
    If (MouseX() > mouse_right_limit) Or (MouseX() < mouse_left_limit) Or (MouseY() > mouse_bottom_limit) Or (MouseY() < mouse_top_limit)
        MoveMouse viewport_center_x, viewport_center_y
    EndIf
End Function

Function UpdateEnemies() 
	For enemy.ENEMIES = Each ENEMIES
		
		enemy\oldX = EntityX(enemy\collider)
		enemy\oldZ = EntityZ(enemy\collider)
		
		PositionEntity enemy\obj, EntityX(enemy\collider), EntityY(enemy\collider) - 0.4, EntityZ(enemy\collider)	
		RotateEntity enemy\obj, 0, EntityYaw(enemy\collider), EntityRoll(enemy\collider)
		
		MoveEntity(enemy\collider, 0,1,0)
		MoveEntity(collider, 0,1,0)		
		
		If EntityVisible(enemy\collider, camera) Then
			PointEntity enemy\collider, camera
			RotateEntity enemy\collider, 0, EntityYaw(enemy\collider), EntityRoll(enemy\collider)
			If EntityDistance(enemy\obj, camera) > 1.5 Then 
				MoveEntity enemy\collider, 0, 0, enemy\speed
				;Animate2(enemy\obj, AnimTime(enemy\obj), 0, 14, 0.3)
			Else
				;Animate2(enemy\obj, AnimTime(enemy\obj), 32, 44, 0.2)			
			EndIf
			BlurTimer = 200
		EndIf 
		
		MoveEntity(enemy\collider, 0,-1,0)
		MoveEntity(collider, 0,-1,0)	
		
		If EntityCollided(enemy\collider,hit_map) Then
			enemy\yspeed = 0
		Else
			enemy\yspeed = enemy\yspeed - 0.004
			TranslateEntity enemy\collider,0,enemy\yspeed,0	
		EndIf
					 
	Next
End Function 

Function CreateEnemy.ENEMIES(nx#,ny#,nz#,texture) 
	If CurrEnemy <> Null Then
		FreeEntity CurrEnemy\collider
		FreeEntity CurrEnemy\obj
		Delete CurrEnemy
		
		CurrEnemy = Null
	EndIf
	
	enemy.ENEMIES = New ENEMIES
	
	enemy\obj = LoadAnimMesh("GFX\mental.b3d");CopyMesh(mentalmesh)
	ScaleEntity enemy\obj, 0.17,0.17,0.17
	
	enemy\collider = CreatePivot()
	PositionEntity enemy\collider, nx,ny,nz
	PositionEntity enemy\obj, nx,ny,nz	
	EntityType enemy\collider, hit_player
	EntityRadius enemy\collider, 0.3
	
	;enemy\collider2 = CreatePivot()
	;EntityRadius enemy\collider2, 2	
	
	EntityTexture enemy\obj, texture 
	
	Return enemy.ENEMIES
End Function



Function CreateGlimpse.GLIMPSES(nx#,ny#,nz#,texture)
	g.glimpses = New GLIMPSES
	g\obj= CreateSprite()
	EntityTexture g\obj, texture
	ScaleSprite g\obj, 0.3,0.3
	PositionEntity g\obj ,nx,ny,nz
End Function

Function UpdateGlimpses()
	For g.glimpses = Each GLIMPSES
		If PlayerFloor-1 = Int((-EntityY(g\obj)-0.5)/2) Then 
			If Distance2(EntityX(g\obj),EntityY(collider),EntityZ(g\obj))<2.3 And EntityVisible(g\obj, camera) Then
				EmitSound(NoSFX, g\obj)
				
				FreeEntity g\obj
				Delete g				
			EndIf
		EndIf
	Next
End Function

Function CreateGlimpses()
	Local FloorX#, FloorY#, FloorZ#
	
	
	For i = 1 To flooramount-1
		If FloorActions(i) = 0 And Rand(7)=1 Then 
			FloorX# =4
			FloorY#=-(i-1)*2-1.0
			
			If Floor(i/2.0)=Ceil(i/2.0) Then ;parillinen
				FloorZ=-6.55
				StartX = 7.2 
				EndX = 0.8
			Else ;pariton
				FloorZ=-0.3
				StartX = 0.8
				EndX = 7.2
			EndIf
			
			CreateGlimpse(Rand(startx,endx), FloorY,FloorZ, GlimpseTextures(Rand(0,1)))
			
		EndIf 
	Next
	
	
End Function 



Function Animate2(ent, curr#, start, quit, speed#)
	SetAnimTime ent, curr + speed
	If AnimTime(ent) > quit Then SetAnimTime ent, start
	If AnimTime(ent) < start Then SetAnimTime ent, quit
End Function 


Function CreateMap(floors)
	
	map0 = LoadMesh("GFX\map0.x")
	map = LoadMesh("GFX\map.x")
	map1 = LoadMesh("GFX\map1.x")
	map2 = LoadMesh("GFX\map2.x")
	map3 = LoadMesh("GFX\map3.x")
	map4 = LoadMesh("GFX\map4.x")
	map5 = LoadMesh("GFX\map5.x")	
	map6 = LoadMesh("GFX\map6.x")	
	map7 = LoadMesh("GFX\maze.x")	
	
	;EntityTexture map, tex1
	
	door = CreateCube()
	ScaleEntity(door, 0.5,1,0.5)
	EntityType door, hit_map
	PositionEntity(door, -3.5, -1, -0.5)
	
	doortexture = LoadTexture("GFX\door.jpg")
	EntityTexture(door, doortexture)
	
	FloorActions(1)=ACT_PROCEED
	FloorTimer(1)=1
	
	If Rand(2)=1 Then
		temp=Rand(3,4)
		FloorActions(temp)=ACT_RADIO2
		FloorTimer(temp)=1			
	EndIf
	
	If Rand(3)<3 Then
		temp=Rand(5,6)
		FloorActions(temp)=ACT_RADIO3
		FloorTimer(temp)=1
	EndIf	
	
	FloorActions(7)=ACT_LOCK
	FloorTimer(7)=1	
	
	If Rand(2)=1 Then
		temp=Rand(8,9)
		FloorActions(temp)=ACT_RADIO4
		FloorTimer(temp)=1
	EndIf
	
	temp=Rand(10,11)
	FloorActions(temp)=ACT_BREATH
	FloorTimer(temp)=1
	
	temp=Rand(12,13)
	FloorActions(temp)=ACT_STEPS
	FloorTimer(temp)=1
	
	;If Rand(5)<5 Then 
		temp = Rand(10,19)
		FloorActions(temp)=ACT_FLASH
		FloorTimer(temp)=Rand(1,3)
	;EndIf
	
	temp = Rand(20,22)
	FloorActions(temp)=ACT_LIGHTS
	FloorTimer(temp)=1
	
	Select Rand(4)
		Case 1
			temp = Rand(25,28)
			FloorActions(temp)=ACT_TRICK1
			FloorTimer(temp)=1		
		Case 2
			temp = Rand(25,28)
			FloorActions(temp)=ACT_TRICK2
			FloorTimer(temp)=1		
	End Select 
	
	
	temp = Rand(29,33)
	FloorActions(temp)=ACT_RUN
	FloorTimer(temp)=1
	
	temp = Rand(34,37)
	FloorActions(temp)=ACT_173
	FloorTimer(temp)=1
	
	temp = Rand(40,60)
	FloorActions(temp)=ACT_RUN
	FloorTimer(temp)=1
	temp = Rand(40,60)
	FloorActions(temp)=ACT_ROAR
	FloorTimer(temp)=1		
	temp = Rand(40,60)
	FloorActions(temp)=ACT_TRAP
	FloorTimer(temp)=1			
	temp = Rand(40,60)
	FloorActions(temp)=ACT_FLASH
	FloorTimer(temp)=1
	
	randact=0
	For i = 0 To 8
		Select Rand(10)
			Case 1,9
				randact=ACT_CELL 
			Case 2
				randact=ACT_FLASH
			Case 3
				randact = ACT_TRICK1
			Case 4
				randact = ACT_TRICK2
			Case 5
				randact = ACT_BREATH
			Case 6
				randact= ACT_STEPS
			Case 7
				randact = ACT_TRAP
			Case 8
				randact = ACT_ROAR
		End Select 
	
		temper = False
		Repeat
			temp = Rand(25,69)
			If FloorActions(temp)=0 Then
				FloorActions(temp)=randact
				FloorTimer(temp)=1
				temper = True
			EndIf
		Until temper=True
	Next
	
	randact=0
	For i = 0 To 60
		Select Rand(10)
			Case 1,9
				randact=ACT_CELL 
			Case 2
				randact=ACT_LIGHTS
			Case 3
				randact = ACT_RUN
			Case 3
				randact = ACT_TRICK1
			Case 4
				randact = ACT_TRICK2
			Case 5
				randact = ACT_BREATH
			Case 6
				randact= ACT_STEPS
			Case 7
				randact = ACT_TRAP
			Case 8
				randact = ACT_ROAR
		End Select 
		
		temper = False
		Repeat
			temp = Rand(75,200)
			If FloorActions(temp)=0 Then
				FloorActions(temp)=randact
				FloorTimer(temp)=1
				temper = True
			EndIf
		Until temper=True
	Next	
	
	temp = Rand(150,200)
	FloorActions(temp)=ACT_DARKNESS
	FloorTimer(temp)=1
	
	For i = 0 To floors-1
		If i = 0 Then
			temp = CopyMesh(map0)
		Else
			Select FloorActions(i+1)
				Case ACT_173
					temp = CopyMesh(map2)
				Case ACT_CELL
					temp = CopyMesh(map1)	
				Case ACT_TRICK1
					temp = CopyMesh(map4)		
				Case ACT_TRICK2
					temp = CopyMesh(map5)
				Case ACT_FLASH, ACT_RUN, ACT_WALK, ACT_LIGHTS, ACT_TRAP, ACT_LOCK
					temp  = CopyMesh(map)
				Case 0
					Select Rand(20)
						Case 1,2
							temp = CopyMesh(map1)
						Case 3,4
							temp = CopyMesh(map2)
						Case 5,6
							temp = CopyMesh(map3)
						Case 7
							temp = CopyMesh(map4)
						Case 8
							temp = CopyMesh(map5)
						Case 9
							temp = CopyMesh(map6)
						Case 10
							If i > 40 Then temp = CopyMesh(map7) Else temp = CopyMesh(map) 
						Default
							temp = CopyMesh(map)
					End Select
				Default 
					temp = CopyMesh(map)		
			End Select 
		EndIf
		
		EntityPickMode temp, 2
		EntityType temp, hit_map

		If Floor(i/2.0)=Ceil(i/2.0) Then ;parillinen
			PositionEntity(temp, 0,-i*2,0)		
		Else ;pariton
			TurnEntity temp, 0, 180, 0
			PositionEntity(temp,8,-i*2,-7)		
		EndIf
	Next
	
	DrawFloorMarkers()
	
	FreeEntity map0
	FreeEntity map
	FreeEntity map1
	FreeEntity map2
	FreeEntity map3
	FreeEntity map4
	FreeEntity map5
	FreeEntity map6
	FreeEntity map7
	
End Function

Function DrawFloorMarkers()
	
	SetFont font 
	For i = 1 To flooramount
		number$=""
		
		Select Rand(600)
			Case 1
				number = ""
			Case 2
				number = Rand(33,122)
			Case 3
				number = "NIL"	
			Case 4
				number = "?"
			Case 5
				number = "NO"
			Case 6
				number = "stop"
			Default
				number = (i+1)
		End Select
		
		If i > 140 Then
			number =""
			For n = 1 To Rand(4)
				number = number + Chr(Rand(33,122))
			Next
		EndIf
		
		FloorNumberTexture(i)=CreateTexture(512,512) 	
		
		cube = CreateCube()
		ScaleEntity cube, 0.25,0.25,0.25
		
		Cls
		DrawImage sign, 0,0
		Color 0,0,0
		
		Text(256,256,number,True,True)
		
		CopyRect(0,0,512,512,0,0,BackBuffer(),TextureBuffer(FloorNumberTexture(i)) )
		
		EntityTexture cube,FloorNumberTexture(i)
		
		If Floor(i/2.0)=Ceil(i/2.0) Then 
			PositionEntity(cube, -0.24,-i*2-0.6,-0.5)
		Else
			PositionEntity(cube,7.4+0.6+0.24,-i*2-0.6,-7+0.5)
		EndIf
		
	Next
	SetFont font1
	
	SetBuffer BackBuffer() 	
	
End Function 


Function CheckHit()
	For enemy.ENEMIES = Each ENEMIES
		
		If EntityDistance(enemy\obj, collider) < 30 Then
			If playerweapon <> 0 Then
				If EntityCollided(Righthand, hit_head) Then End 
			Else
				If EntityCollided(RightHand, hit_head) Then End
			EndIf 
		EndIf 
			 
	Next	
End Function 

Function UpdateFloors()

	Local FloorX#, FloorY#, FloorZ#

	For i = 0 To flooramount-1
		If FloorTimer(i)>0 Then
		
			FloorX# =4
			FloorY#=-1-(i-1)*2
			
			If Floor(i/2.0)=Ceil(i/2.0) Then ;parillinen
				FloorZ=-6.75
				StartX = 7.5 
				EndX = 0.5
			Else ;pariton
				FloorZ=-0.25
				StartX = 0.5 
				EndX = 7.5
			EndIf
			
			Select FloorActions(i)

				Case ACT_LIGHTS
					If FloorTimer(i)>1 Then
						FloorTimer(i) = FloorTimer(i)+1
						
						If FloorTimer(i) >100 Then
							Animate2(CurrEnemy\obj,AnimTime(CurrEnemy\obj),1,14,0.15)	
							If Distance2(EntityX(CurrEnemy\collider),EntityY(CurrEnemy\collider),EntityZ(CurrEnemy\collider))<0.8 Then
								KillTimer = max(KillTimer,1)
							EndIf
						EndIf
						
						If FloorTimer(i) = 100 Then CurrEnemy = CreateEnemy(EndX, FloorY-0.5, FloorZ, mental) : CurrEnemy\speed = 0.01
						If FloorTimer(i) = 210 Then PlaySound (FireOn)
						If FloorTimer(i) = 250 Then AmbientLight Brightness,Brightness,Brightness
						If FloorTimer(i) = 290  Then PlaySound(HorrorSFX(2))
						If FloorTimer(i) = 450 Then
							FreeEntity CurrEnemy\collider
							FreeEntity CurrEnemy\obj
							Delete CurrEnemy
							
							FloorTimer(i)=0
						EndIf
					EndIf
				Case ACT_RUN
					If FloorTimer(i) > 1 Then
						FloorTimer(i) = FloorTimer(i)+1
						
						If FloorTimer(i) >100 Then
							If Distance2(EntityX(CurrEnemy\collider),EntityY(CurrEnemy\collider),EntityZ(CurrEnemy\collider))<0.8 Then
								KillTimer = max(KillTimer,1)
							EndIf
						EndIf
						
						If FloorTimer(i) = 100 Then 
							CurrEnemy = CreateEnemy(EndX, FloorY-0.5, FloorZ,mental) : CurrEnemy\speed = 0.035
						ElseIf FloorTimer(i) = 130 Or FloorTimer(i) = 260 Or FloorTimer(i) = 380 ;valot syttyy
							PlaySound(HorrorSFX(0))
							CameraFogRange camera, 1, 20
							AmbientLight Brightness,Brightness,Brightness
							CurrEnemy\speed = 0.0
						ElseIf FloorTimer(i) = 170 Or FloorTimer(i) = 300 ; valot sammuu
							CurrEnemy\speed = 0.03
							CameraFogRange camera, 1, 2.5
							AmbientLight 15,15,15
						ElseIf FloorTimer(i) = 450
							CameraFogRange camera, 1, 2.5
							AmbientLight Brightness,Brightness,Brightness
							FreeEntity CurrEnemy\collider
							FreeEntity CurrEnemy\obj
							Delete CurrEnemy
							
							FloorTimer(i)=0	
						EndIf
					
					EndIf
				Case ACT_173
					If FloorTimer(i) > 1 Then
						FloorTimer(i) = FloorTimer(i)+1
						
						If EntityVisible(CurrEnemy\collider, camera) And FloorTimer(i)>150 Then
							If EntityInView(CurrEnemy\collider, camera) Then
								CurrEnemy\speed = 0.0
								Animate2(CurrEnemy\obj,AnimTime(CurrEnemy\obj),206,250,0.05)
								If FloorTimer(i)<10000 Then PlaySound(HorrorSFX(2)) : FloorTimer(i)=10001
							Else
								CurrEnemy\speed = 0.02
								If Distance2(EntityX(CurrEnemy\collider),EntityY(CurrEnemy\collider),EntityZ(CurrEnemy\collider))<0.8 Then
									KillTimer = max(KillTimer,1)
									
									FloorTimer(i)=0
								EndIf
							EndIf	
						Else
							CurrEnemy\speed = 0.0
						EndIf
						If (FloorTimer(i) Mod 660) = 15 Then EmitSound(DontlookSFX, CurrEnemy\collider)
					EndIf				
					
					If PlayerFloor > i Then
						FreeEntity CurrEnemy\collider
						FreeEntity CurrEnemy\obj
						Delete CurrEnemy
						
						FloorTimer(i)=0
					EndIf
				Case ACT_TRAP
					If FloorTimer(i) > 2 Then 
						DebugLog  FloorTimer(i)
						FloorTimer(i) = FloorTimer(i) +1
						If Distance2(EntityX(CurrEnemy\collider),EntityY(CurrEnemy\collider),EntityZ(CurrEnemy\collider))<0.8 Then
							KillTimer = max(KillTimer,1)
						EndIf	
						If FloorTimer(i) = 500 Then
							FreeEntity CurrObject
							PlaySound StoneSFX
						ElseIf FloorTimer(i) = 1000
							FreeEntity CurrEnemy\collider
							FreeEntity CurrEnemy\obj
							Delete CurrEnemy
							FloorTimer(i) = 0
						EndIf
					EndIf
			End Select
		EndIf
	Next

		
	If FloorTimer(PlayerFloor)>0 Then
		FloorX# =4
		FloorY#=-1-(PlayerFloor-1)*2
		
		If Floor(PlayerFloor/2.0)=Ceil(PlayerFloor/2.0) Then ;parillinen
			FloorZ=-6.5
			StartX = 7.5 
			EndX = 0.5
		Else ;pariton
			FloorZ=-0.5
			StartX = 0.5 
			EndX = 7.5	
		EndIf
		
		Select FloorActions(PlayerFloor)
			Case ACT_PROCEED
				FloorTimer(PlayerFloor) = FloorTimer(PlayerFloor)+1
				If FloorTimer(PlayerFloor) =150 Then
					PlaySound RadioSFX(0)
					FloorTimer(PlayerFloor)=0
				EndIf
			Case ACT_RADIO2
				PlaySound RadioSFX(1)	;signal seems to be getting weaker
				FloorTimer(PlayerFloor)=0
			Case ACT_RADIO3		
				PlaySound RadioSFX(2)	;good luck
				FloorTimer(PlayerFloor)=0
			Case ACT_RADIO4		
				PlaySound RadioSFX(3)	;MÖRKÖILYÄ	
				FloorTimer(PlayerFloor)=0
			Case ACT_FLASH ;mörkö vilahtaa käytävän päässä
				If FloorTimer(PlayerFloor)= 1 Then
					If Distance2(EndX, FloorY, FloorZ)<1.5 Then
						CurrEnemy = CreateEnemy(EndX, FloorY-0.5, FloorZ,mental)
						PlaySound(HorrorSFX(Rand(0,2)))
						FloorTimer(PlayerFloor) = 5
					EndIf
				ElseIf FloorTimer(PlayerFloor)= 2
					If Distance2(FloorX, FloorY, FloorZ)<1.5 Then
						CurrEnemy = CreateEnemy(FloorX, FloorY-0.5, FloorZ,mental)
						PlaySound(HorrorSFX(Rand(0,2)))
						FloorTimer(PlayerFloor) = 5
					EndIf
				ElseIf FloorTimer(PlayerFloor)= 3
					If Distance2(startX, FloorY, FloorZ)<1.5 Then
						CurrEnemy = CreateEnemy(startX, FloorY-0.5, FloorZ,mental)
						PlaySound(HorrorSFX(Rand(0,2)))
						FloorTimer(PlayerFloor) = 5
					EndIf	
				Else
					FloorTimer(PlayerFloor) = FloorTimer(PlayerFloor)+1
					If FloorTimer(PlayerFloor) > 30 Then
					 	FreeEntity CurrEnemy\collider
						FreeEntity CurrEnemy\obj						
						Delete CurrEnemy
						
						FloorTimer(PlayerFloor)=0
					EndIf
				EndIf
			Case ACT_LIGHTS
				If FloorTimer(PlayerFloor)= 1 Then
					If Distance2(FloorX, FloorY, FloorZ)<1.0 Then
						PlaySound(HorrorSFX(1))
						PlaySound(FireOff)
						FloorTimer(PlayerFloor) = 2
						AmbientLight 25,25,25
					EndIf
				EndIf
			Case ACT_STEPS
				If FloorTimer(PlayerFloor)= 1 Then
					FloorTimer(PlayerFloor)= 2
				ElseIf FloorTimer(PlayerFloor)< 3000
					If Distance2(EndX, FloorY, FloorZ)<6 Then 
						PositionEntity SoundEmitter,FloorX+(FloorX-EndX)*1.1,FloorY,FloorZ
						FloorTimer(PlayerFloor) = FloorTimer(PlayerFloor)+1
						If FloorTimer(PlayerFloor) Mod 150 < Rand(50) Then
							EmitSound(loudstepsound, SoundEmitter)
							FloorTimer(PlayerFloor) = 51
						EndIf
					EndIf
				EndIf		
			Case ACT_BREATH  
				If FloorTimer( PlayerFloor)= 1 Then
					FloorTimer(PlayerFloor)= 2
				ElseIf FloorTimer(PlayerFloor)< 3000
					If Distance2(EndX, FloorY, FloorZ)<7 Then 
						PositionEntity SoundEmitter,FloorX+(FloorX-EndX)*1.1,FloorY,FloorZ
						FloorTimer(PlayerFloor) = FloorTimer(PlayerFloor)+1
						If (FloorTimer(PlayerFloor) Mod 600) < 10 Then
							EmitSound(BreathSFX, SoundEmitter)
							FloorTimer(PlayerFloor) = 11
						EndIf
					EndIf
				EndIf	
			Case ACT_RUN
				If FloorTimer(PlayerFloor)= 1 Then
					If Distance2(FloorX, FloorY, FloorZ)<3.0 Then
						PlaySound(HorrorSFX(1))
						PlaySound(FireOff)
						FloorTimer(PlayerFloor) = 2
						AmbientLight 25,25,25
					EndIf
				EndIf
			Case ACT_173
				If FloorTimer(PlayerFloor)= 1 Then
					If Floor(PlayerFloor/2.0)=Ceil(PlayerFloor/2.0) Then ;parillinen 
						CurrEnemy = CreateEnemy(startx-1.8,FloorY-0.5,FloorZ-6.0,tex173)  
					Else ;pariton
						CurrEnemy = CreateEnemy(startx+1.8,FloorY-0.5,FloorZ+6.0,tex173)  
					EndIf
					EntityFX CurrEnemy\obj, 8
					CurrEnemy\speed = 0.0
					FloorTimer(PlayerFloor) = 2
				EndIf
			Case ACT_CELL
				If FloorTimer(PlayerFloor)= 1 Then
					If Floor(PlayerFloor/2.0)=Ceil(PlayerFloor/2.0) Then ;parillinen 
						CurrEnemy = CreateEnemy(startx-4.5,FloorY-0.5,FloorZ+3.2,tex173)  
					Else ;pariton
						CurrEnemy = CreateEnemy(startx+4.5,FloorY-0.5,FloorZ-3.2,tex173)  
					EndIf
					EntityFX CurrEnemy\obj, 8
					CurrEnemy\speed = 0.0
					FloorTimer(PlayerFloor) = 2
				Else
					FloorTimer(PlayerFloor) = FloorTimer(PlayerFloor)+1
					If CurrEnemy <> Null Then 
						
						Animate2(CurrEnemy\obj,AnimTime(CurrEnemy\obj),206,250,0.05)
						
						If Abs(EntityX(collider)-EntityX(CurrEnemy\collider))<0.025 And Rand(40)=1 Then 
							If CurrEnemy\speed = 0 Then PlaySound (HorrorSFX(2))
							SetAnimTime(CurrEnemy\obj, 0)
							MoveEntity CurrEnemy\collider, 0, 0, 1.5
							FloorTimer(PlayerFloor)= 0
						EndIf
					EndIf
					If (FloorTimer(PlayerFloor) Mod 610)= 5 Then 
						If Floor(PlayerFloor/2.0)=Ceil(PlayerFloor/2.0) Then ;parillinen 
							PositionEntity(SoundEmitter,startx-4.5,FloorY-0.5,FloorZ+3.2)
						Else ;pariton
							PositionEntity(SoundEmitter,startx+4.5,FloorY-0.5,FloorZ-3.2)  
						EndIf
						EmitSound(BreathSFX, SoundEmitter)
					EndIf 
				EndIf
			Case ACT_LOCK			
				If Distance2(FloorX, FloorY, FloorZ)<1.0 Then
					wall = CreateCube()
					ScaleEntity(wall, 0.5,1,0.5)
					If Floor(PlayerFloor/2.0)=Ceil(PlayerFloor/2.0) Then ;parillinen
						PositionEntity (wall, startx-0.5, FloorY, FloorZ)
					Else ;pariton
						PositionEntity (wall, startx+0.5, FloorY, FloorZ)	
					EndIf
					EntityTexture wall,brickwalltexture	
					EntityType wall, hit_map
					EmitSound (StoneSFX,wall)
					FloorTimer(PlayerFloor)= 0
				EndIf
			Case ACT_TRICK1
				If FloorTimer(PlayerFloor)=1 Then 
					If Floor(PlayerFloor/2.0)=Ceil(PlayerFloor/2.0) Then ;parillinen 
						If Distance2(startx-1.5,FloorY-0.5,FloorZ-5.0)<0.25 Then
							CurrEnemy = CreateEnemy(startx-1.5,FloorY-0.5,FloorZ-2.0,tex173)  
							CurrEnemy\speed = 0.01
							EntityFX CurrEnemy\obj, 8
							FloorTimer(PlayerFloor)=2
							PlaySound(HorrorSFX(2))
						EndIf
					Else ;pariton
						If	Distance2(startx+1.5,FloorY-0.5,FloorZ+5.0)<0.25 Then
							CurrEnemy = CreateEnemy(startx+1.5,FloorY-0.5,FloorZ+2.0,tex173)  
							CurrEnemy\speed = 0.01
							EntityFX CurrEnemy\obj, 8
							FloorTimer(PlayerFloor)=2
							PlaySound(HorrorSFX(2))
						EndIf
					EndIf
				Else
					If Distance2(EntityX(CurrEnemy\collider),EntityY(CurrEnemy\collider),EntityZ(CurrEnemy\collider))<0.8 Then
						KillTimer = max(KillTimer,1)
					EndIf
				EndIf 
			Case ACT_TRICK2
				If FloorTimer(PlayerFloor)=1 Then 	
					If Floor(PlayerFloor/2.0)=Ceil(PlayerFloor/2.0) Then ;parillinen 
						If	Distance2(startx+0.5,FloorY-0.5,FloorZ-5.0)<0.25 Then
							CurrEnemy = CreateEnemy(startx+0.5,FloorY-0.5,FloorZ-2.0,tex173)  
							CurrEnemy\speed = 0.01
							EntityFX CurrEnemy\obj, 8
							FloorTimer(PlayerFloor)=2
							PlaySound(HorrorSFX(2))
						EndIf
					Else ;pariton
						If Distance2(startx-0.5,FloorY-0.5,FloorZ+5.0)<0.25 Then
							CurrEnemy = CreateEnemy(startx-0.5,FloorY-0.5,FloorZ+2.0,tex173)  
							CurrEnemy\speed = 0.01
							EntityFX CurrEnemy\obj, 8
							FloorTimer(PlayerFloor)=2
							PlaySound(HorrorSFX(2))
						EndIf
					EndIf		
				Else
					If Distance2(EntityX(CurrEnemy\collider),EntityY(CurrEnemy\collider),EntityZ(CurrEnemy\collider))<0.8 Then
						KillTimer = max(KillTimer,1)
					EndIf	
				EndIf
			Case ACT_TRAP
				If FloorTimer(PlayerFloor)=1 Then 
					CurrObject = CreateCube()
					ScaleEntity(CurrObject, 0.5,1,0.5)
					If Floor(PlayerFloor/2.0)=Ceil(PlayerFloor/2.0) Then ;parillinen
						PositionEntity (CurrObject, endx+0.5, FloorY, FloorZ)
					Else ;pariton
						PositionEntity (CurrObject, endx-0.5, FloorY, FloorZ)	
					EndIf
					EntityTexture CurrObject,brickwalltexture	
					EntityType CurrObject, hit_map
					FloorTimer(PlayerFloor)= 2
				ElseIf FloorTimer(PlayerFloor)=2
					If Distance2(FloorX, FloorY, FloorZ)<1.0 Then	
						CurrEnemy = CreateEnemy(startX, FloorY-0.5, FloorZ,mental)
						CurrEnemy\speed = 0.01
						PlaySound(HorrorSFX(Rand(0,2)))
						FloorTimer(PlayerFloor) = 3
					EndIf
				EndIf
			Case ACT_ROAR
				If FloorTimer(PlayerFloor)= 1 Then
					If Distance2(EndX, FloorY, FloorZ)<6 Then 
						PositionEntity SoundEmitter,FloorX,FloorY-3,FloorZ
						EmitSound(RoarSFX, SoundEmitter)
						FloorTimer(PlayerFloor) = 51
					EndIf
				Else
					FloorTimer(PlayerFloor) = FloorTimer(PlayerFloor)+1
					If FloorTimer(PlayerFloor)<370 Then
						TranslateEntity(collider, Rnd(-0.005,0.005),Rnd(-0.005,0.005),Rnd(-0.005,0.005))
						TurnEntity(camera, Rnd(-1,1), Rnd(-1,1), Rnd(-1,1))
					Else
						FloorTimer(PlayerFloor) = 0
					EndIf
				EndIf
			Case ACT_DARKNESS
				If FloorTimer(PlayerFloor)= 1 Then
					If Distance2(FloorX, FloorY, FloorZ)<1.0 Then
						wall = CreateCube()
						ScaleEntity(wall, 0.5,1,0.5)
						If Floor(PlayerFloor/2.0)=Ceil(PlayerFloor/2.0) Then ;parillinen
							PositionEntity (wall, startx-0.5, FloorY, FloorZ)
						Else ;pariton
							PositionEntity (wall, startx+0.5, FloorY, FloorZ)	
						EndIf
						EntityTexture wall,brickwalltexture	
						EntityType wall, hit_map
						
						wall = CreateCube()
						ScaleEntity(wall, 0.5,1,0.5)
						If Floor(PlayerFloor/2.0)=Ceil(PlayerFloor/2.0) Then ;parillinen
							PositionEntity (wall, ENDX+0.5, FloorY, FloorZ)
						Else ;pariton
							PositionEntity (wall, endx-0.5, FloorY, FloorZ)	
						EndIf
						EntityTexture wall,brickwalltexture	
						EntityType wall, hit_map					
						
						EmitSound (StoneSFX,wall)
						FloorTimer(PlayerFloor)= 2
					EndIf		
				ElseIf FloorTimer(PlayerFloor)<600
					FloorTimer(PlayerFloor) = FloorTimer(PlayerFloor)+1
					temp#=max(Brightness-(FloorTimer(PlayerFloor)/600.0)*Brightness,10)
					AmbientLight temp,temp,temp
					
					If FloorTimer(PlayerFloor) = 600 Then
						CurrEnemy = CreateEnemy(FloorX, FloorY-0.5, FloorZ,mental)
						CurrEnemy\speed = 0.01
						PlaySound(HorrorSFX(Rand(0,2)))
						FloorTimer(PlayerFloor) = 601
					EndIf
				Else
					If Distance2(EntityX(CurrEnemy\collider),EntityY(CurrEnemy\collider),EntityZ(CurrEnemy\collider))<0.7 Then
						KillTimer = max(KillTimer,1)
					EndIf
				EndIf
		End Select
		
	EndIf
End Function

Function Distance2#(x1#, y1#, z1#) 
	Local vxcomp# = Abs(x1 - EntityX(collider))
	Local vycomp# = Abs(y1 - EntityY(collider))
	Local vzcomp# = Abs(z1 - EntityZ(collider))
	Return Sqr(vxcomp * vxcomp + vycomp * vycomp + vzcomp * vzcomp)
End Function

Function Kill()
	If Not isCrouch Then  
	If KillTimer = 1 Then PlaySound DeathSFX
	
	KillTimer=KillTimer+1
	
	AmbientLight 255-KillTimer, 100-KillTimer, 100-KillTimer
	RotateEntity camera, -KillTimer, EntityYaw(camera), EntityRoll(collider)-(KillTimer/2)
	
	If KillTimer>90 Then 
		If PlayerFloor > 130 Then
			Select Rand(7)
				Case 1
					RuntimeError("NO")
				Case 2
					RuntimeError("It's not about whether you die or not, it's about when you die.")
				Case 3
					RuntimeError("NICE")
				Case 4
					RuntimeError("welcome to NIL")	
			End Select 
		EndIf
		
		End
	EndIf
	EndIf
End Function 

Function GetINIString$(file$, section$, parameter$)
	Local TemporaryString$ = ""
	Local f = ReadFile(file)
	
	While Not Eof(f)
		If Lower(ReadLine(f)) = "[" + Lower(section) + "]" Then
			Repeat
				TemporaryString = ReadLine(f)
			 	If Lower(Trim(Left(TemporaryString, max(Instr(TemporaryString, "=") - 1, 0)))) = Lower(parameter) Then
					CloseFile f
					Return Trim( Right(TemporaryString,Len(TemporaryString)-Instr(TemporaryString,"=")) )
				EndIf
			Until Left(TemporaryString, 1) = "[" Or Eof(f)
			CloseFile f
			Return ""
		EndIf
	Wend
	
	CloseFile f
End Function

Function GetINIInt$(file$, section$, parameter$)
	Local Stri$= Trim(GetINIString(file$, section$, parameter$))
	If Lower(Stri) = "true" Then
		Return 1
	ElseIf Lower(Stri) = "false"
		Return 0
	Else
		Return Int(Stri)
	EndIf
End Function

Function GetINIFloat$(file$, section$, parameter$)
	Return Float(GetINIString(file$, section$, parameter$))
End Function


Function WordWrap%(A$,X,Y,W,H,Leading=0, center = 0)
	;Display A$ starting at X,Y - no wider than W and no taller than H (all in pixels).
	;Leading is optional extra vertical spacing in pixels
	;To Do (if needed): force break if single word is too big to fit on line (currently function will hang if this happens)
	LinesShown=0
	StrHeight=StringHeight(A$)+Leading
	FittedText$ = ""
	While Len(A)>0
		space=Instr(A$," ")
		If space=0 Then space=Len(A$)
		temp$=Left$(A$,space)
		trimmed$=Trim$(temp);we might ignore a final space
		extra=0;we haven't ignored it yet
		;ignore final space if doing so would make a word fit at end of line:
		If (StringWidth (b$+temp$)>W) And (StringWidth (b$+trimmed$)<=W) Then temp=trimmed:extra=1
		If StringWidth (b$+temp$)>W Then;too big, so print what will fit
			If center Then Text X + (W Shr 1),LinesShown * StrHeight + Y,b$,True Else Text X,LinesShown * StrHeight + Y,b$
			FittedText = FittedText + b
			LinesShown=LinesShown+1
			b$=""
		Else;append it to B$ (which will eventually be printed) and remove it from A$
			b$=b$+temp$
			A$=Right$(A$,Len(A$)-(Len(temp$)+extra))
		EndIf
		If ((LinesShown+1)*StrHeight)>H Then Exit;the next line would be too tall, so leave
	Wend
	If (b$<>"")And((LinesShown+1)<=H) Then 
		If center Then Text X + (W Shr 1),LinesShown * StrHeight + Y,b$,True Else Text X,LinesShown * StrHeight + Y,b$
		;Text X,LinesShown*StrHeight+Y,b$;print any remaining text if it'll fit vertically
		FittedText = FittedText + b
	EndIf
	Return Len(FittedText)
End Function




;~IDEal Editor Parameters:
;~F#136#13A#13E#142#16E#193#1B1#1C7#1E4#2D2#30D#31B#4BF#4C6#4E0
;~C#Blitz3D