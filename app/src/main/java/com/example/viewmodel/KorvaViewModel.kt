package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SampleProjects
import com.example.data.SpriteAsset
import com.example.data.SpriteLibrary
import com.example.engine.KorExporter
import com.example.engine.SpriteSheetGenerator
import com.example.model.*
import com.example.model.vfx.*
import com.example.engine.vfx.KorvBinarySerializer
import com.example.engine.vfx.VFXSimulationEngine
import com.example.data.VFXPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class AppScreen {
    HOME,
    STUDIO
}

class KorvaViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _project = MutableStateFlow(SampleProjects.createDefaultProject())
    val project: StateFlow<KorProject> = _project.asStateFlow()

    // ----------------------------------------------------
    // VFX & PARTICLE ENGINE STATE (Korva VFX System 1.0)
    // ----------------------------------------------------
    val vfxEngine = VFXSimulationEngine(VFXPresets.createFireExplosion())

    private val _currentVFXEffect = MutableStateFlow(VFXPresets.createFireExplosion())
    val currentVFXEffect: StateFlow<VFXEffect> = _currentVFXEffect.asStateFlow()

    private val _vfxActiveParticleCount = MutableStateFlow(0)
    val vfxActiveParticleCount: StateFlow<Int> = _vfxActiveParticleCount.asStateFlow()

    private val _isVfxSimulating = MutableStateFlow(true)
    val isVfxSimulating: StateFlow<Boolean> = _isVfxSimulating.asStateFlow()

    private val _selectedEmitterIndex = MutableStateFlow(0)
    val selectedEmitterIndex: StateFlow<Int> = _selectedEmitterIndex.asStateFlow()

    private var vfxSimJob: Job? = null

    private val _selectedLayerId = MutableStateFlow<String?>(null)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    private val _currentFrame = MutableStateFlow(0f)
    val currentFrame: StateFlow<Float> = _currentFrame.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _activeTool = MutableStateFlow(EditorTool.SELECT)
    val activeTool: StateFlow<EditorTool> = _activeTool.asStateFlow()

    private val _viewportZoom = MutableStateFlow(1.0f)
    val viewportZoom: StateFlow<Float> = _viewportZoom.asStateFlow()

    private val _viewportPan = MutableStateFlow(Offset.Zero)
    val viewportPan: StateFlow<Offset> = _viewportPan.asStateFlow()

    // Studio Options
    private val _onionSkinEnabled = MutableStateFlow(true)
    val onionSkinEnabled: StateFlow<Boolean> = _onionSkinEnabled.asStateFlow()

    private val _onionSkinPastFrames = MutableStateFlow(2)
    val onionSkinPastFrames: StateFlow<Int> = _onionSkinPastFrames.asStateFlow()

    private val _onionSkinFutureFrames = MutableStateFlow(2)
    val onionSkinFutureFrames: StateFlow<Int> = _onionSkinFutureFrames.asStateFlow()

    private val _gridVisible = MutableStateFlow(true)
    val gridVisible: StateFlow<Boolean> = _gridVisible.asStateFlow()

    private val _snapToGrid = MutableStateFlow(false)
    val snapToGrid: StateFlow<Boolean> = _snapToGrid.asStateFlow()

    private val _gridSize = MutableStateFlow(32)
    val gridSize: StateFlow<Int> = _gridSize.asStateFlow()

    // Panel states for compact dynamic layout
    private val _isTopBarCollapsed = MutableStateFlow(false)
    val isTopBarCollapsed: StateFlow<Boolean> = _isTopBarCollapsed.asStateFlow()

    private val _isLeftToolbarCollapsed = MutableStateFlow(false)
    val isLeftToolbarCollapsed: StateFlow<Boolean> = _isLeftToolbarCollapsed.asStateFlow()

    private val _isRightInspectorCollapsed = MutableStateFlow(true)
    val isRightInspectorCollapsed: StateFlow<Boolean> = _isRightInspectorCollapsed.asStateFlow()

    private val _isBottomTimelineCollapsed = MutableStateFlow(false)
    val isBottomTimelineCollapsed: StateFlow<Boolean> = _isBottomTimelineCollapsed.asStateFlow()

    private val _timelineHeightDp = MutableStateFlow(175f)
    val timelineHeightDp: StateFlow<Float> = _timelineHeightDp.asStateFlow()

    // Next-Gen Timeline Advanced State
    private val _timelineZoom = MutableStateFlow(1.0f) // 0.5f to 3.0f
    val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

    private val _timelineFitToScreen = MutableStateFlow(true)
    val timelineFitToScreen: StateFlow<Boolean> = _timelineFitToScreen.asStateFlow()

    private val _timelineSnapToKeyframes = MutableStateFlow(true)
    val timelineSnapToKeyframes: StateFlow<Boolean> = _timelineSnapToKeyframes.asStateFlow()

    private val _selectedKeyframe = MutableStateFlow<Pair<String, Int>?>(null) // LayerId, Frame
    val selectedKeyframe: StateFlow<Pair<String, Int>?> = _selectedKeyframe.asStateFlow()

    private val _timelineViewMode = MutableStateFlow(0) // 0: DopeSheet Tracks, 1: Motion Graph & Curve Curves
    val timelineViewMode: StateFlow<Int> = _timelineViewMode.asStateFlow()

    private val _timelineShowSubTracks = MutableStateFlow(false) // Position, Scale, Rotation channels
    val timelineShowSubTracks: StateFlow<Boolean> = _timelineShowSubTracks.asStateFlow()

    private val _workAreaEnabled = MutableStateFlow(false)
    val workAreaEnabled: StateFlow<Boolean> = _workAreaEnabled.asStateFlow()

    private val _workAreaStart = MutableStateFlow(0)
    val workAreaStart: StateFlow<Int> = _workAreaStart.asStateFlow()

    private val _workAreaEnd = MutableStateFlow(24)
    val workAreaEnd: StateFlow<Int> = _workAreaEnd.asStateFlow()

    private val _timeDisplayFormat = MutableStateFlow(0) // 0: Frames (F 12), 1: Timecode (00:00.50s), 2: Percentage (50%)
    val timeDisplayFormat: StateFlow<Int> = _timeDisplayFormat.asStateFlow()

    private val _inspectorTab = MutableStateFlow(0) // 0: Properties, 1: Layers
    val inspectorTab: StateFlow<Int> = _inspectorTab.asStateFlow()

    // Viewport Advanced Overlays & Modes
    private val _showMotionTrajectory = MutableStateFlow(true)
    val showMotionTrajectory: StateFlow<Boolean> = _showMotionTrajectory.asStateFlow()

    private val _showSafeZones = MutableStateFlow(false)
    val showSafeZones: StateFlow<Boolean> = _showSafeZones.asStateFlow()

    private val _showRuleOfThirds = MutableStateFlow(false)
    val showRuleOfThirds: StateFlow<Boolean> = _showRuleOfThirds.asStateFlow()

    private val _canvasBgMode = MutableStateFlow(0) // 0: Solid, 1: Checkerboard, 2: Deep Obsidian, 3: Light Gray
    val canvasBgMode: StateFlow<Int> = _canvasBgMode.asStateFlow()

    // Dialogs
    private val _showSpriteLibrary = MutableStateFlow(false)
    val showSpriteLibrary: StateFlow<Boolean> = _showSpriteLibrary.asStateFlow()

    private val _showEasingDialog = MutableStateFlow(false)
    val showEasingDialog: StateFlow<Boolean> = _showEasingDialog.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _showProjectDialog = MutableStateFlow(false)
    val showProjectDialog: StateFlow<Boolean> = _showProjectDialog.asStateFlow()

    private val _showResolutionDialog = MutableStateFlow(false)
    val showResolutionDialog: StateFlow<Boolean> = _showResolutionDialog.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>("Welcome to Korva Animation Studio")
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Undo / Redo history
    private val undoStack = mutableListOf<KorProject>()
    private val redoStack = mutableListOf<KorProject>()

    private var playbackJob: Job? = null
    private var pingPongForward = true

    init {
        // Default select top layer
        _project.value.layers.lastOrNull()?.let {
            _selectedLayerId.value = it.id
        }

        // Start High-Speed 60fps VFX Engine simulation loop
        startVFXSimulation()
    }

    fun startVFXSimulation() {
        vfxSimJob?.cancel()
        _isVfxSimulating.value = true
        vfxSimJob = viewModelScope.launch(Dispatchers.Default) {
            var lastTime = System.currentTimeMillis()
            while (isActive && _isVfxSimulating.value) {
                val now = System.currentTimeMillis()
                val dt = (now - lastTime).coerceIn(1, 50) / 1000f
                lastTime = now

                vfxEngine.update(dt)
                _vfxActiveParticleCount.value = vfxEngine.pool.activeCount

                delay(16) // ~60 FPS
            }
        }
    }

    fun toggleVFXSimulation() {
        if (_isVfxSimulating.value) {
            _isVfxSimulating.value = false
            vfxSimJob?.cancel()
        } else {
            startVFXSimulation()
        }
    }

    fun resetVFXSimulation() {
        vfxEngine.reset()
        _vfxActiveParticleCount.value = 0
    }

    fun selectVFXPreset(preset: VFXEffect) {
        val cloned = preset.deepCopy()
        _currentVFXEffect.value = cloned
        vfxEngine.setEffectTarget(cloned)
        _selectedEmitterIndex.value = 0
        postStatus("Loaded VFX Preset: ${preset.name}")
    }

    fun createNewCustomVFX(name: String = "Custom VFX") {
        val newEffect = VFXPresets.createEmptyBlankEffect(name)
        _currentVFXEffect.value = newEffect
        vfxEngine.setEffectTarget(newEffect)
        _selectedEmitterIndex.value = 0
        postStatus("Created New Blank VFX: $name")
    }

    fun setSelectedEmitterIndex(index: Int) {
        _selectedEmitterIndex.value = index.coerceIn(0, (_currentVFXEffect.value.emitters.size - 1).coerceAtLeast(0))
    }

    fun updateSelectedEmitter(mutate: (VFXEmitter) -> Unit) {
        val effect = _currentVFXEffect.value
        val idx = _selectedEmitterIndex.value
        if (idx in 0 until effect.emitters.size) {
            mutate(effect.emitters[idx])
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateVFXName(name: String) {
        val effect = _currentVFXEffect.value
        effect.name = name
        val updated = effect.deepCopy()
        _currentVFXEffect.value = updated
        vfxEngine.setEffectTarget(updated)
    }

    fun updateVFXDuration(duration: Float) {
        val effect = _currentVFXEffect.value
        effect.duration = duration.coerceIn(0.2f, 30f)
        val updated = effect.deepCopy()
        _currentVFXEffect.value = updated
        vfxEngine.setEffectTarget(updated)
    }

    fun updateVFXLooping(looping: Boolean) {
        val effect = _currentVFXEffect.value
        effect.looping = looping
        val updated = effect.deepCopy()
        _currentVFXEffect.value = updated
        vfxEngine.setEffectTarget(updated)
    }

    fun updateVFXBlendMode(blendMode: BlendMode) {
        val effect = _currentVFXEffect.value
        effect.blendMode = blendMode
        val updated = effect.deepCopy()
        _currentVFXEffect.value = updated
        vfxEngine.setEffectTarget(updated)
    }

    fun updateVFXTimeScale(scale: Float) {
        val effect = _currentVFXEffect.value
        effect.timeScale = scale.coerceIn(0.1f, 3.0f)
        val updated = effect.deepCopy()
        _currentVFXEffect.value = updated
        vfxEngine.setEffectTarget(updated)
    }

    fun prewarmVFX(warmSeconds: Float = 1.0f) {
        vfxEngine.prewarm(warmSeconds)
        postStatus("Prewarmed VFX simulation ($warmSeconds s)")
    }

    fun addModuleToEmitter(index: Int, type: ModuleTypeId) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val exists = emitter.modules.any { it.typeId == type }
            if (exists) {
                postStatus("Module ${type.displayName} already exists on this emitter")
                return
            }
            when (type) {
                ModuleTypeId.LIFETIME -> emitter.modules.add(LifetimeModule())
                ModuleTypeId.VELOCITY -> emitter.modules.add(VelocityModule())
                ModuleTypeId.GRAVITY -> emitter.modules.add(GravityModule(gravity = -9.81f, damping = 0.98f))
                ModuleTypeId.ROTATION -> emitter.modules.add(RotationModule())
                ModuleTypeId.SCALE_OVER_LIFETIME -> emitter.modules.add(ScaleModule())
                ModuleTypeId.COLOR_OVER_LIFETIME -> emitter.modules.add(ColorModule())
                ModuleTypeId.ALPHA_OVER_LIFETIME -> emitter.modules.add(AlphaModule())
                ModuleTypeId.TURBULENCE -> emitter.modules.add(TurbulenceModule(strength = 25f, frequency = 0.05f, scrollSpeed = 1.0f, useCurlNoise = true))
                ModuleTypeId.DRAG -> emitter.modules.add(DragModule(linearDrag = 0.5f, quadraticDrag = 0.01f))
                ModuleTypeId.VORTEX -> emitter.modules.add(VortexModule(center = Vector2(0f, 0f), vortexStrength = 50f, radialPull = 15f))
                ModuleTypeId.ATTRACTOR -> emitter.modules.add(AttractorModule(targetPosition = Vector2(0f, 0f), strength = 60f))
                ModuleTypeId.COLLISION -> emitter.modules.add(CollisionModule(floorY = 140f, restitution = 0.65f, friction = 0.8f))
                ModuleTypeId.FLIPBOOK -> emitter.modules.add(FlipbookModule(columns = 4, rows = 4, totalFrames = 16, frameRate = 30f, loop = true))
                ModuleTypeId.VELOCITY_ALIGNMENT -> emitter.modules.add(VelocityAlignmentModule(stretchFactor = 0.03f))
                ModuleTypeId.COLOR_BY_SPEED -> emitter.modules.add(ColorBySpeedModule(minSpeed = 0f, maxSpeed = 120f))
                ModuleTypeId.TRAIL -> emitter.modules.add(TrailModule(segmentInterval = 0.02f, trailLifetime = 0.3f, maxPoints = 16))
            }
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
            postStatus("Added Module: ${type.displayName}")
        }
    }

    fun removeModuleFromEmitter(index: Int, type: ModuleTypeId) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val removed = emitter.modules.removeAll { it.typeId == type }
            if (removed) {
                val updated = effect.deepCopy()
                _currentVFXEffect.value = updated
                vfxEngine.setEffectTarget(updated)
                postStatus("Removed Module: ${type.displayName}")
            }
        }
    }

    fun updateTurbulenceModule(index: Int, strength: Float, frequency: Float, scrollSpeed: Float, useCurl: Boolean) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val turb = emitter.findTurbulenceModule()
            if (turb != null) {
                turb.strength = strength
                turb.frequency = frequency
                turb.scrollSpeed = scrollSpeed
                turb.useCurlNoise = useCurl
            } else {
                emitter.modules.add(TurbulenceModule(strength, frequency, scrollSpeed, useCurl))
            }
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateDragModule(index: Int, linearDrag: Float, quadraticDrag: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val drag = emitter.findDragModule()
            if (drag != null) {
                drag.linearDrag = linearDrag
                drag.quadraticDrag = quadraticDrag
            } else {
                emitter.modules.add(DragModule(linearDrag, quadraticDrag))
            }
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateVortexModule(index: Int, strength: Float, radialPull: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val vortex = emitter.findVortexModule()
            if (vortex != null) {
                vortex.vortexStrength = strength
                vortex.radialPull = radialPull
            } else {
                emitter.modules.add(VortexModule(vortexStrength = strength, radialPull = radialPull))
            }
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateAttractorModule(index: Int, targetX: Float, targetY: Float, strength: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val attr = emitter.findAttractorModule()
            if (attr != null) {
                attr.targetPosition.x = targetX
                attr.targetPosition.y = targetY
                attr.strength = strength
            } else {
                emitter.modules.add(AttractorModule(targetPosition = Vector2(targetX, targetY), strength = strength))
            }
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateCollisionModule(index: Int, floorY: Float, restitution: Float, friction: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val coll = emitter.findCollisionModule()
            if (coll != null) {
                coll.floorY = floorY
                coll.restitution = restitution
                coll.friction = friction
            } else {
                emitter.modules.add(CollisionModule(floorY, restitution, friction))
            }
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateFlipbookModule(index: Int, columns: Int, rows: Int, totalFrames: Int, fps: Float, loop: Boolean) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val flip = emitter.findFlipbookModule()
            if (flip != null) {
                flip.columns = columns
                flip.rows = rows
                flip.totalFrames = totalFrames
                flip.frameRate = fps
                flip.loop = loop
            } else {
                emitter.modules.add(FlipbookModule(columns, rows, totalFrames, fps, loop))
            }
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateVelocityAlignmentModule(index: Int, stretchFactor: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val align = emitter.findVelocityAlignmentModule()
            if (align != null) {
                align.stretchFactor = stretchFactor
            } else {
                emitter.modules.add(VelocityAlignmentModule(stretchFactor))
            }
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateColorBySpeedModule(index: Int, minSpeed: Float, maxSpeed: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val spd = emitter.findColorBySpeedModule()
            if (spd != null) {
                spd.minSpeed = minSpeed
                spd.maxSpeed = maxSpeed
            } else {
                emitter.modules.add(ColorBySpeedModule(minSpeed = minSpeed, maxSpeed = maxSpeed))
            }
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateTrailModule(index: Int, segmentInterval: Float, trailLifetime: Float, maxPoints: Int) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val trail = emitter.findTrailModule()
            if (trail != null) {
                trail.segmentInterval = segmentInterval
                trail.trailLifetime = trailLifetime
                trail.maxPoints = maxPoints
            } else {
                emitter.modules.add(TrailModule(segmentInterval, trailLifetime, maxPoints))
            }
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateSubEmitters(index: Int, onBirth: Int, onDeath: Int, onCollision: Int) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            emitter.onBirthSubEmitter = onBirth
            emitter.onDeathSubEmitter = onDeath
            emitter.onCollisionSubEmitter = onCollision
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateEmitterSpawnRate(index: Int, rate: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            effect.emitters[index].spawnRate = rate.coerceIn(0f, 500f)
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateEmitterLifetime(index: Int, lifetime: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            effect.emitters[index].particleLifetime = lifetime.coerceIn(0.1f, 10f)
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateEmitterShape(index: Int, shape: ShapeType) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            effect.emitters[index].shapeType = shape
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateEmitterSpeed(index: Int, speedMin: Float, speedMax: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            effect.emitters[index].speedMin = speedMin.coerceAtLeast(0f)
            effect.emitters[index].speedMax = speedMax.coerceAtLeast(speedMin)
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateEmitterSpread(index: Int, angle: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            effect.emitters[index].spreadAngle = angle.coerceIn(0f, 360f)
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateEmitterBurst(index: Int, burstCount: Int, burstInterval: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            effect.emitters[index].burstCount = burstCount.coerceAtLeast(0)
            effect.emitters[index].burstInterval = burstInterval.coerceAtLeast(0.05f)
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateEmitterScaleRange(index: Int, minScale: Float, maxScale: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            effect.emitters[index].baseScaleMin = Vector2(minScale, minScale)
            effect.emitters[index].baseScaleMax = Vector2(maxScale, maxScale)
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun updateEmitterGravity(index: Int, gravity: Float) {
        val effect = _currentVFXEffect.value
        if (index in 0 until effect.emitters.size) {
            val emitter = effect.emitters[index]
            val grav = emitter.findGravityModule()
            if (grav != null) {
                grav.gravity = gravity
            } else {
                emitter.modules.add(GravityModule(gravity = gravity))
            }
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
        }
    }

    fun addEmitterToCurrentEffect(name: String = "New Emitter", shapeType: ShapeType = ShapeType.CIRCLE) {
        val effect = _currentVFXEffect.value
        val newEmitter = VFXEmitter(
            name = name,
            shapeType = shapeType,
            shapeSize = Vector2(15f, 15f),
            spawnRate = 50f,
            particleLifetime = 1.5f,
            speedMin = 20f,
            speedMax = 60f,
            spreadAngle = 60f
        )
        newEmitter.modules.add(GravityModule(gravity = -2f))
        newEmitter.modules.add(ScaleModule(VFXCurve(interpolation = InterpolationType.CUBIC).apply {
            addKeyframe(0f, 0.2f)
            addKeyframe(0.5f, 1f)
            addKeyframe(1f, 0f)
        }))
        newEmitter.modules.add(ColorModule(VFXGradient().apply {
            addKey(0f, ColorRGBA(255, 200, 50, 1f))
            addKey(1f, ColorRGBA(255, 50, 0, 0f))
        }))

        effect.emitters.add(newEmitter)
        val updated = effect.deepCopy()
        _currentVFXEffect.value = updated
        vfxEngine.setEffectTarget(updated)
        _selectedEmitterIndex.value = updated.emitters.size - 1
        postStatus("Added Emitter: $name")
    }

    fun removeSelectedEmitter() {
        val effect = _currentVFXEffect.value
        if (effect.emitters.size <= 1) {
            postStatus("Cannot remove the only emitter in effect")
            return
        }
        val idx = _selectedEmitterIndex.value
        if (idx in 0 until effect.emitters.size) {
            val removed = effect.emitters.removeAt(idx)
            _selectedEmitterIndex.value = 0
            val updated = effect.deepCopy()
            _currentVFXEffect.value = updated
            vfxEngine.setEffectTarget(updated)
            postStatus("Removed emitter: ${removed.name}")
        }
    }

    fun exportKorvBinary(): ByteArray {
        return KorvBinarySerializer.serialize(_currentVFXEffect.value)
    }

    fun importKorvBinary(bytes: ByteArray): Boolean {
        val effect = KorvBinarySerializer.deserialize(bytes)
        return if (effect != null) {
            _currentVFXEffect.value = effect
            vfxEngine.setEffectTarget(effect)
            _selectedEmitterIndex.value = 0
            postStatus("Loaded .korv Binary: ${effect.name} (${bytes.size} bytes)")
            true
        } else {
            postStatus("Failed to deserialize .korv binary")
            false
        }
    }

    fun saveKorvFileToDevice(context: Context): String? {
        return try {
            val bytes = exportKorvBinary()
            val safeName = _currentVFXEffect.value.name.replace("\\s+".toRegex(), "_").lowercase()
            val filename = "${safeName}.korv"
            val file = File(context.filesDir, filename)
            file.writeBytes(bytes)
            postStatus("Saved binary VFX: $filename (${bytes.size} bytes, CRC32 Verified)")
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            postStatus("Error saving .korv binary: ${e.localizedMessage}")
            null
        }
    }

    private fun pushUndoState() {
        undoStack.add(_project.value.copy())
        if (undoStack.size > 30) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(_project.value.copy())
            val prev = undoStack.removeAt(undoStack.lastIndex)
            _project.value = prev
            postStatus("Undo applied")
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(_project.value.copy())
            val next = redoStack.removeAt(redoStack.lastIndex)
            _project.value = next
            postStatus("Redo applied")
        }
    }

    fun postStatus(msg: String) {
        _statusMessage.value = msg
    }

    // Playback control with ultra-smooth delta-time loop
    fun togglePlay() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        val total = _project.value.totalFrames.toFloat()
        if (_currentFrame.value >= total - 1f) {
            _currentFrame.value = 0f
            pingPongForward = true
        }
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch(Dispatchers.Default) {
            var lastNanoTime = System.nanoTime()
            var currentPos = _currentFrame.value

            while (isActive && _isPlaying.value) {
                val now = System.nanoTime()
                val deltaSeconds = (now - lastNanoTime) / 1_000_000_000f
                lastNanoTime = now

                if (kotlin.math.abs(_currentFrame.value - currentPos) > 1.0f) {
                    currentPos = _currentFrame.value
                }

                val proj = _project.value
                val baseFps = proj.fps.toFloat().coerceAtLeast(1f)
                val speed = proj.speedMultiplier.coerceIn(0.1f, 5.0f)
                val effectiveFps = baseFps * speed

                val frameDelta = deltaSeconds * effectiveFps
                val totalFloat = proj.totalFrames.toFloat().coerceAtLeast(1f)

                when (proj.loopMode) {
                    LoopMode.REPEAT -> {
                        currentPos += frameDelta
                        if (currentPos >= totalFloat) {
                            currentPos %= totalFloat
                        }
                    }
                    LoopMode.ONCE -> {
                        currentPos += frameDelta
                        if (currentPos >= totalFloat - 1f) {
                            currentPos = totalFloat - 1f
                            _currentFrame.value = currentPos
                            _isPlaying.value = false
                            break
                        }
                    }
                    LoopMode.PING_PONG -> {
                        if (pingPongForward) {
                            currentPos += frameDelta
                            if (currentPos >= totalFloat - 1f) {
                                currentPos = totalFloat - 1f
                                pingPongForward = false
                            }
                        } else {
                            currentPos -= frameDelta
                            if (currentPos <= 0f) {
                                currentPos = 0f
                                pingPongForward = true
                            }
                        }
                    }
                }
                _currentFrame.value = currentPos.coerceIn(0f, (totalFloat - 1f).coerceAtLeast(0f))

                // Smooth delay targeting ~60-120fps refresh rate
                delay(12L)
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun stop() {
        pause()
        _currentFrame.value = 0f
    }

    fun scrubToFrame(frame: Float) {
        val total = _project.value.totalFrames.toFloat()
        _currentFrame.value = frame.coerceIn(0f, (total - 1f).coerceAtLeast(0f))
    }

    fun stepForward() {
        val total = _project.value.totalFrames
        val next = kotlin.math.floor(_currentFrame.value) + 1f
        _currentFrame.value = if (next >= total) 0f else next
    }

    fun stepBackward() {
        val total = _project.value.totalFrames
        val prev = kotlin.math.ceil(_currentFrame.value) - 1f
        _currentFrame.value = if (prev < 0f) (total - 1).toFloat() else prev
    }

    fun jumpToStart() {
        _currentFrame.value = 0f
    }

    fun jumpToEnd() {
        _currentFrame.value = (_project.value.totalFrames - 1).coerceAtLeast(0).toFloat()
    }

    fun setFps(fps: Int) {
        pushUndoState()
        _project.update { it.copy(fps = fps) }
        postStatus("FPS set to $fps")
    }

    fun setSpeedMultiplier(mult: Float) {
        _project.update { it.copy(speedMultiplier = mult) }
        postStatus("Playback speed: ${mult}x")
    }

    fun setLoopMode(mode: LoopMode) {
        _project.update { it.copy(loopMode = mode) }
        postStatus("Loop mode: ${mode.label}")
    }

    fun setAccelerationType(accel: AccelerationType) {
        _project.update { it.copy(accelerationType = accel) }
        postStatus("Scene acceleration: ${accel.label}")
    }

    fun setResolution(res: ResolutionPreset) {
        pushUndoState()
        _project.update { it.copy(resolution = res) }
        postStatus("Resolution: ${res.label} (${res.width}x${res.height})")
    }

    fun setTotalFrames(frames: Int) {
        pushUndoState()
        val validFrames = frames.coerceIn(4, 300)
        _project.update { it.copy(totalFrames = validFrames) }
        if (_currentFrame.value >= validFrames) {
            _currentFrame.value = (validFrames - 1).toFloat()
        }
        if (validFrames > 30) {
            _timelineFitToScreen.value = false
        }
        postStatus("Total frames: $validFrames")
    }

    // Tools & Viewport
    fun setActiveTool(tool: EditorTool) {
        _activeTool.value = tool
        postStatus("Tool: ${tool.label}")
    }

    fun selectLayer(id: String?) {
        _selectedLayerId.value = id
    }

    fun updateViewportZoom(delta: Float) {
        val newZoom = (_viewportZoom.value * delta).coerceIn(0.2f, 8.0f)
        _viewportZoom.value = newZoom
    }

    fun setViewportZoom(zoom: Float) {
        _viewportZoom.value = zoom.coerceIn(0.2f, 8.0f)
    }

    fun updateViewportPan(dragAmount: Offset) {
        _viewportPan.value += dragAmount
    }

    fun resetViewport() {
        _viewportZoom.value = 1.0f
        _viewportPan.value = Offset.Zero
        postStatus("Viewport centered")
    }

    // Options toggles
    fun toggleOnionSkin() {
        _onionSkinEnabled.value = !_onionSkinEnabled.value
        postStatus("Onion skin: ${if (_onionSkinEnabled.value) "On" else "Off"}")
    }

    fun setOnionSkinPastFrames(count: Int) {
        _onionSkinPastFrames.value = count.coerceIn(1, 5)
    }

    fun setOnionSkinFutureFrames(count: Int) {
        _onionSkinFutureFrames.value = count.coerceIn(1, 5)
    }

    fun toggleGrid() {
        _gridVisible.value = !_gridVisible.value
    }

    fun toggleSnapToGrid() {
        _snapToGrid.value = !_snapToGrid.value
        postStatus("Grid Snap: ${if (_snapToGrid.value) "Enabled" else "Disabled"}")
    }

    fun toggleMotionTrajectory() {
        _showMotionTrajectory.value = !_showMotionTrajectory.value
        postStatus("Motion Path: ${if (_showMotionTrajectory.value) "Visible" else "Hidden"}")
    }

    fun toggleSafeZones() {
        _showSafeZones.value = !_showSafeZones.value
        postStatus("Safe Frames: ${if (_showSafeZones.value) "Enabled" else "Disabled"}")
    }

    fun toggleRuleOfThirds() {
        _showRuleOfThirds.value = !_showRuleOfThirds.value
        postStatus("Rule of Thirds: ${if (_showRuleOfThirds.value) "Enabled" else "Disabled"}")
    }

    fun cycleCanvasBg() {
        _canvasBgMode.value = (_canvasBgMode.value + 1) % 4
        val modeName = when (_canvasBgMode.value) {
            0 -> "Solid Scene Color"
            1 -> "Checkerboard Alpha"
            2 -> "Dark Obsidian Grid"
            else -> "Light Canvas"
        }
        postStatus("Canvas Background: $modeName")
    }

    fun updateSelectedLayerColor(color: Long) {
        val selected = getSelectedLayer() ?: return
        val currentStyle = selected.shapeStyle ?: ShapeStyle()
        updateLayerStyle(selected.id, currentStyle.copy(fillColor = color))
        postStatus("Updated color")
    }

    fun updateSelectedLayerOpacity(opacity: Float) {
        val selected = getSelectedLayer() ?: return
        updateLayerOpacity(selected.id, opacity)
    }

    fun stepSelectedLayerOpacity(delta: Float) {
        val selected = getSelectedLayer() ?: return
        val newOpacity = (selected.opacity + delta).coerceIn(0.05f, 1f)
        updateLayerOpacity(selected.id, newOpacity)
        postStatus("Opacity: ${(newOpacity * 100).toInt()}%")
    }

    fun toggleTopBar() {
        _isTopBarCollapsed.value = !_isTopBarCollapsed.value
        postStatus(if (_isTopBarCollapsed.value) "Header closed" else "Header opened")
    }

    fun toggleLeftToolbar() {
        _isLeftToolbarCollapsed.value = !_isLeftToolbarCollapsed.value
    }

    fun toggleRightInspector() {
        _isRightInspectorCollapsed.value = !_isRightInspectorCollapsed.value
    }

    fun toggleBottomTimeline() {
        _isBottomTimelineCollapsed.value = !_isBottomTimelineCollapsed.value
    }

    fun toggleZenMode() {
        val anyOpen = !_isLeftToolbarCollapsed.value || !_isRightInspectorCollapsed.value || !_isBottomTimelineCollapsed.value || !_isTopBarCollapsed.value
        _isLeftToolbarCollapsed.value = anyOpen
        _isRightInspectorCollapsed.value = anyOpen
        _isBottomTimelineCollapsed.value = anyOpen
        _isTopBarCollapsed.value = anyOpen
        postStatus(if (anyOpen) "Zen Mode: Maximized Stage" else "Panels Restored")
    }

    fun setInspectorTab(tab: Int) {
        _inspectorTab.value = tab
    }

    // Dialog triggers
    fun openSpriteLibrary(open: Boolean) { _showSpriteLibrary.value = open }
    fun openEasingDialog(open: Boolean) { _showEasingDialog.value = open }
    fun openExportDialog(open: Boolean) { _showExportDialog.value = open }
    fun openProjectDialog(open: Boolean) { _showProjectDialog.value = open }
    fun openResolutionDialog(open: Boolean) { _showResolutionDialog.value = open }

    // Layer & Keyframe manipulation
    fun getSelectedLayer(): AnimationLayer? {
        val id = _selectedLayerId.value ?: return null
        return _project.value.layers.find { it.id == id }
    }

    fun addSpriteFromLibrary(asset: SpriteAsset) {
        pushUndoState()
        val newLayer = SpriteLibrary.createLayerFromAsset(asset, 0f, 0f)
        val currentLayers = _project.value.layers.toMutableList()
        currentLayers.add(newLayer.copy(zIndex = currentLayers.size))
        _project.update { it.copy(layers = currentLayers) }
        _selectedLayerId.value = newLayer.id
        postStatus("Added sprite: ${asset.name}")
    }

    fun addCustomImageLayer(uri: Uri, context: Context) {
        pushUndoState()
        val id = UUID.randomUUID().toString()
        val newLayer = AnimationLayer(
            id = id,
            name = "Sprite Image",
            type = LayerType.CUSTOM_IMAGE,
            customImageUri = uri.toString(),
            width = 120f,
            height = 120f,
            zIndex = _project.value.layers.size,
            keyframes = listOf(
                Keyframe(frame = 0, x = 0f, y = 0f, rotation = 0f, scaleX = 1f, scaleY = 1f)
            )
        )
        val currentLayers = _project.value.layers.toMutableList()
        currentLayers.add(newLayer)
        _project.update { it.copy(layers = currentLayers) }
        _selectedLayerId.value = newLayer.id
        postStatus("Imported image layer")
    }

    fun addShapeLayer(kind: ShapeKind) {
        pushUndoState()
        val id = UUID.randomUUID().toString()
        val newLayer = AnimationLayer(
            id = id,
            name = kind.displayName,
            type = LayerType.VECTOR_SHAPE,
            shapeKind = kind,
            width = 90f,
            height = 90f,
            zIndex = _project.value.layers.size,
            shapeStyle = ShapeStyle(
                fillColor = 0xFFA855F7,
                strokeColor = 0xFFFFFFFF,
                strokeWidth = 2.5f,
                hasFill = true,
                hasStroke = true
            ),
            keyframes = listOf(
                Keyframe(frame = 0, x = 0f, y = 0f, rotation = 0f, scaleX = 1f, scaleY = 1f)
            )
        )
        val currentLayers = _project.value.layers.toMutableList()
        currentLayers.add(newLayer)
        _project.update { it.copy(layers = currentLayers) }
        _selectedLayerId.value = newLayer.id
        postStatus("Created shape: ${kind.displayName}")
    }

    fun duplicateSelectedLayer() {
        val selected = getSelectedLayer() ?: return
        pushUndoState()
        val copyId = UUID.randomUUID().toString()
        val copied = selected.copy(
            id = copyId,
            name = "${selected.name} (Copy)",
            keyframes = selected.keyframes.map { it.copy(x = it.x + 20f, y = it.y + 20f) },
            zIndex = _project.value.layers.size
        )
        val updated = _project.value.layers.toMutableList().apply { add(copied) }
        _project.update { it.copy(layers = updated) }
        _selectedLayerId.value = copyId
        postStatus("Duplicated layer: ${selected.name}")
    }

    fun deleteSelectedLayer() {
        val selected = getSelectedLayer() ?: return
        pushUndoState()
        val updated = _project.value.layers.filter { it.id != selected.id }
        _project.update { it.copy(layers = updated) }
        _selectedLayerId.value = updated.lastOrNull()?.id
        postStatus("Deleted layer: ${selected.name}")
    }

    fun toggleLayerVisibility(layerId: String) {
        pushUndoState()
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
        }
        _project.update { it.copy(layers = updated) }
    }

    fun toggleLayerLock(layerId: String) {
        pushUndoState()
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it
        }
        _project.update { it.copy(layers = updated) }
    }

    fun moveLayerZ(layerId: String, delta: Int) {
        val layers = _project.value.layers.toMutableList()
        val index = layers.indexOfFirst { it.id == layerId }
        if (index == -1) return
        val targetIndex = (index + delta).coerceIn(0, layers.size - 1)
        if (targetIndex != index) {
            pushUndoState()
            val item = layers.removeAt(index)
            layers.add(targetIndex, item)
            // Re-assign zIndex
            val reIndexed = layers.mapIndexed { idx, layer -> layer.copy(zIndex = idx) }
            _project.update { it.copy(layers = reIndexed) }
        }
    }

    fun updateLayerName(layerId: String, newName: String) {
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(name = newName) else it
        }
        _project.update { it.copy(layers = updated) }
    }

    fun updateLayerOpacity(layerId: String, opacity: Float) {
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(opacity = opacity.coerceIn(0f, 1f)) else it
        }
        _project.update { it.copy(layers = updated) }
    }

    fun updateLayerStyle(layerId: String, style: ShapeStyle) {
        pushUndoState()
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(shapeStyle = style) else it
        }
        _project.update { it.copy(layers = updated) }
    }

    fun updateLayerPivot(layerId: String, pivotX: Float, pivotY: Float) {
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(pivotX = pivotX.coerceIn(0f, 1f), pivotY = pivotY.coerceIn(0f, 1f)) else it
        }
        _project.update { it.copy(layers = updated) }
    }

    fun updateLayerSize(layerId: String, width: Float, height: Float) {
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(width = width.coerceAtLeast(10f), height = height.coerceAtLeast(10f)) else it
        }
        _project.update { it.copy(layers = updated) }
    }

    // Keyframe Management
    fun addOrUpdateKeyframeOnCurrentFrame(
        x: Float? = null,
        y: Float? = null,
        rotation: Float? = null,
        scaleX: Float? = null,
        scaleY: Float? = null,
        opacity: Float? = null,
        easing: EasingType? = null
    ) {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return

        val frameInt = _currentFrame.value.toInt()
        val currentTransform = EasingFunctions.evaluateLayerAtFrame(layer, _currentFrame.value)

        val existingKf = layer.keyframes.find { it.frame == frameInt }
        val newKf = Keyframe(
            frame = frameInt,
            x = x ?: existingKf?.x ?: currentTransform.x,
            y = y ?: existingKf?.y ?: currentTransform.y,
            rotation = rotation ?: existingKf?.rotation ?: currentTransform.rotation,
            scaleX = scaleX ?: existingKf?.scaleX ?: currentTransform.scaleX,
            scaleY = scaleY ?: existingKf?.scaleY ?: currentTransform.scaleY,
            opacity = opacity ?: existingKf?.opacity ?: currentTransform.opacity,
            easing = easing ?: existingKf?.easing ?: EasingType.EASE_IN_OUT_CUBIC
        )

        val updatedKfs = layer.keyframes.filter { it.frame != frameInt }.toMutableList().apply {
            add(newKf)
            sortBy { it.frame }
        }

        val updatedLayers = _project.value.layers.map {
            if (it.id == layer.id) it.copy(keyframes = updatedKfs) else it
        }
        _project.update { it.copy(layers = updatedLayers) }
        postStatus("Keyframe set on frame $frameInt")
    }

    fun deleteKeyframeOnCurrentFrame() {
        val layer = getSelectedLayer() ?: return
        val frameInt = _currentFrame.value.toInt()

        if (layer.keyframes.size <= 1) {
            postStatus("Cannot delete only keyframe")
            return
        }

        pushUndoState()
        val updatedKfs = layer.keyframes.filter { it.frame != frameInt }
        if (updatedKfs.size == layer.keyframes.size) {
            postStatus("No keyframe on frame $frameInt")
            return
        }

        val updatedLayers = _project.value.layers.map {
            if (it.id == layer.id) it.copy(keyframes = updatedKfs) else it
        }
        _project.update { it.copy(layers = updatedLayers) }
        postStatus("Removed keyframe on frame $frameInt")
    }

    fun setKeyframeEasing(easing: EasingType) {
        val layer = getSelectedLayer() ?: return
        val frameInt = _currentFrame.value.toInt()
        val existingKf = layer.keyframes.find { it.frame == frameInt }

        if (existingKf != null) {
            pushUndoState()
            val updatedKfs = layer.keyframes.map {
                if (it.frame == frameInt) it.copy(easing = easing) else it
            }
            val updatedLayers = _project.value.layers.map {
                if (it.id == layer.id) it.copy(keyframes = updatedKfs) else it
            }
            _project.update { it.copy(layers = updatedLayers) }
            postStatus("Easing: ${easing.displayName}")
        } else {
            // Add keyframe with this easing
            addOrUpdateKeyframeOnCurrentFrame(easing = easing)
        }
    }

    // Advanced Next-Gen Timeline Actions
    fun setSelectedKeyframe(layerId: String?, frame: Int?) {
        if (layerId != null && frame != null) {
            _selectedKeyframe.value = Pair(layerId, frame)
            _selectedLayerId.value = layerId
            scrubToFrame(frame.toFloat())
        } else {
            _selectedKeyframe.value = null
        }
    }

    fun jumpToPrevKeyframe() {
        val currentF = _currentFrame.value.toInt()
        val allKfFrames = _project.value.layers.flatMap { it.keyframes }.map { it.frame }.distinct().sorted()
        val prevKf = allKfFrames.lastOrNull { it < currentF }
        if (prevKf != null) {
            scrubToFrame(prevKf.toFloat())
            postStatus("Jumped to Keyframe F$prevKf")
        } else if (currentF > 0) {
            jumpToStart()
        }
    }

    fun jumpToNextKeyframe() {
        val currentF = _currentFrame.value.toInt()
        val allKfFrames = _project.value.layers.flatMap { it.keyframes }.map { it.frame }.distinct().sorted()
        val nextKf = allKfFrames.firstOrNull { it > currentF }
        if (nextKf != null) {
            scrubToFrame(nextKf.toFloat())
            postStatus("Jumped to Keyframe F$nextKf")
        } else if (currentF < _project.value.totalFrames - 1) {
            jumpToEnd()
        }
    }

    fun moveKeyframe(layerId: String, oldFrame: Int, newFrame: Int) {
        val validNewFrame = newFrame.coerceIn(0, _project.value.totalFrames - 1)
        if (oldFrame == validNewFrame) return

        pushUndoState()
        val updatedLayers = _project.value.layers.map { layer ->
            if (layer.id == layerId) {
                val targetKf = layer.keyframes.find { it.frame == oldFrame }
                if (targetKf != null) {
                    val filtered = layer.keyframes.filter { it.frame != oldFrame && it.frame != validNewFrame }
                    val movedKf = targetKf.copy(frame = validNewFrame)
                    layer.copy(keyframes = (filtered + movedKf).sortedBy { it.frame })
                } else layer
            } else layer
        }
        _project.update { it.copy(layers = updatedLayers) }
        _selectedKeyframe.value = Pair(layerId, validNewFrame)
        scrubToFrame(validNewFrame.toFloat())
        postStatus("Moved keyframe from F$oldFrame to F$validNewFrame")
    }

    fun nudgeKeyframe(layerId: String, frame: Int, delta: Int) {
        moveKeyframe(layerId, frame, frame + delta)
    }

    fun duplicateKeyframe(layerId: String, fromFrame: Int, toFrame: Int) {
        val validToFrame = toFrame.coerceIn(0, _project.value.totalFrames - 1)
        val layer = _project.value.layers.find { it.id == layerId } ?: return
        val sourceKf = layer.keyframes.find { it.frame == fromFrame } ?: return

        pushUndoState()
        val newKf = sourceKf.copy(frame = validToFrame)
        val updatedKfs = layer.keyframes.filter { it.frame != validToFrame }.toMutableList().apply {
            add(newKf)
            sortBy { it.frame }
        }
        val updatedLayers = _project.value.layers.map {
            if (it.id == layerId) it.copy(keyframes = updatedKfs) else it
        }
        _project.update { it.copy(layers = updatedLayers) }
        _selectedKeyframe.value = Pair(layerId, validToFrame)
        scrubToFrame(validToFrame.toFloat())
        postStatus("Duplicated keyframe to F$validToFrame")
    }

    fun setTimelineZoom(zoom: Float) {
        _timelineZoom.value = zoom.coerceIn(0.4f, 3.5f)
        _timelineFitToScreen.value = false
    }

    fun zoomTimelineIn() {
        _timelineFitToScreen.value = false
        setTimelineZoom(_timelineZoom.value * 1.25f)
    }

    fun zoomTimelineOut() {
        _timelineFitToScreen.value = false
        setTimelineZoom(_timelineZoom.value * 0.8f)
    }

    fun toggleTimelineFitToScreen() {
        _timelineFitToScreen.value = !_timelineFitToScreen.value
        postStatus(if (_timelineFitToScreen.value) "Timeline: Fit to View" else "Timeline: Custom Zoom")
    }

    fun setTimelineFitToScreen(fit: Boolean) {
        _timelineFitToScreen.value = fit
    }

    fun toggleTimelineSnap() {
        _timelineSnapToKeyframes.value = !_timelineSnapToKeyframes.value
        postStatus("Timeline Snap: ${if (_timelineSnapToKeyframes.value) "ON" else "OFF"}")
    }

    fun toggleTimelineViewMode() {
        _timelineViewMode.value = if (_timelineViewMode.value == 0) 1 else 0
        postStatus("Timeline Mode: ${if (_timelineViewMode.value == 0) "DopeSheet Tracks" else "Curves & Motion Graph"}")
    }

    fun toggleSubTracks() {
        _timelineShowSubTracks.value = !_timelineShowSubTracks.value
    }

    fun cycleTimeDisplayFormat() {
        _timeDisplayFormat.value = (_timeDisplayFormat.value + 1) % 3
    }

    fun toggleWorkArea() {
        _workAreaEnabled.value = !_workAreaEnabled.value
        postStatus("Work Area Loop: ${if (_workAreaEnabled.value) "Active" else "Disabled"}")
    }

    fun setWorkAreaRange(start: Int, end: Int) {
        val s = start.coerceIn(0, _project.value.totalFrames - 1)
        val e = end.coerceIn(s + 1, _project.value.totalFrames)
        _workAreaStart.value = s
        _workAreaEnd.value = e
    }

    // Direct Canvas Transform Drag
    fun applyCanvasTranslation(dragDelta: Offset) {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return

        val currentTransform = EasingFunctions.evaluateLayerAtFrame(layer, _currentFrame.value)
        var newX = currentTransform.x + dragDelta.x / _viewportZoom.value
        var newY = currentTransform.y + dragDelta.y / _viewportZoom.value

        if (_snapToGrid.value) {
            val sz = _gridSize.value.toFloat()
            newX = kotlin.math.round(newX / sz) * sz
            newY = kotlin.math.round(newY / sz) * sz
        }

        addOrUpdateKeyframeOnCurrentFrame(x = newX, y = newY)
    }

    fun applyCanvasRotation(angleDeltaDeg: Float) {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return

        val currentTransform = EasingFunctions.evaluateLayerAtFrame(layer, _currentFrame.value)
        val newRot = (currentTransform.rotation + angleDeltaDeg) % 360f
        addOrUpdateKeyframeOnCurrentFrame(rotation = newRot)
    }

    fun applyCanvasScale(scaleDelta: Float) {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return

        val currentTransform = EasingFunctions.evaluateLayerAtFrame(layer, _currentFrame.value)
        val signX = if (currentTransform.scaleX < 0f) -1f else 1f
        val signY = if (currentTransform.scaleY < 0f) -1f else 1f
        val newSx = (kotlin.math.abs(currentTransform.scaleX) * scaleDelta).coerceIn(0.02f, 15f) * signX
        val newSy = (kotlin.math.abs(currentTransform.scaleY) * scaleDelta).coerceIn(0.02f, 15f) * signY
        addOrUpdateKeyframeOnCurrentFrame(scaleX = newSx, scaleY = newSy)
    }

    fun applyCanvasScaleAxis(scaleDeltaX: Float, scaleDeltaY: Float) {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return

        val currentTransform = EasingFunctions.evaluateLayerAtFrame(layer, _currentFrame.value)
        val signX = if (currentTransform.scaleX < 0f) -1f else 1f
        val signY = if (currentTransform.scaleY < 0f) -1f else 1f
        val newSx = (kotlin.math.abs(currentTransform.scaleX) * scaleDeltaX).coerceIn(0.02f, 15f) * signX
        val newSy = (kotlin.math.abs(currentTransform.scaleY) * scaleDeltaY).coerceIn(0.02f, 15f) * signY
        addOrUpdateKeyframeOnCurrentFrame(scaleX = newSx, scaleY = newSy)
    }

    fun setScaleDirectly(scaleX: Float, scaleY: Float) {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return
        val signX = if (scaleX < 0f) -1f else 1f
        val signY = if (scaleY < 0f) -1f else 1f
        val magX = kotlin.math.abs(scaleX).coerceIn(0.02f, 15f)
        val magY = kotlin.math.abs(scaleY).coerceIn(0.02f, 15f)
        addOrUpdateKeyframeOnCurrentFrame(
            scaleX = magX * signX,
            scaleY = magY * signY
        )
    }

    fun resetTransform() {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return
        addOrUpdateKeyframeOnCurrentFrame(
            x = 0f,
            y = 0f,
            rotation = 0f,
            scaleX = 1f,
            scaleY = 1f,
            opacity = 1f
        )
        postStatus("Reset transform for ${layer.name}")
    }

    fun flipLayerHorizontal() {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return
        pushUndoState()
        val currentTransform = EasingFunctions.evaluateLayerAtFrame(layer, _currentFrame.value)
        val newScaleX = -currentTransform.scaleX
        addOrUpdateKeyframeOnCurrentFrame(scaleX = newScaleX)
        postStatus("Flipped ${layer.name} Horizontally (↔)")
    }

    fun flipLayerVertical() {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return
        pushUndoState()
        val currentTransform = EasingFunctions.evaluateLayerAtFrame(layer, _currentFrame.value)
        val newScaleY = -currentTransform.scaleY
        addOrUpdateKeyframeOnCurrentFrame(scaleY = newScaleY)
        postStatus("Flipped ${layer.name} Vertically (↕)")
    }

    fun centerSelectedLayer() {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return
        pushUndoState()
        addOrUpdateKeyframeOnCurrentFrame(x = 0f, y = 0f)
        postStatus("Centered ${layer.name} to (0, 0)")
    }

    fun stepScale(factor: Float) {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return
        val currentTransform = EasingFunctions.evaluateLayerAtFrame(layer, _currentFrame.value)
        val signX = if (currentTransform.scaleX < 0f) -1f else 1f
        val signY = if (currentTransform.scaleY < 0f) -1f else 1f
        val newSx = (kotlin.math.abs(currentTransform.scaleX) * factor).coerceIn(0.02f, 15f) * signX
        val newSy = (kotlin.math.abs(currentTransform.scaleY) * factor).coerceIn(0.02f, 15f) * signY
        addOrUpdateKeyframeOnCurrentFrame(scaleX = newSx, scaleY = newSy)
        val percent = (kotlin.math.abs(newSx) * 100).toInt()
        postStatus("Scale: $percent%")
    }

    fun stepRotation(angleDeltaDeg: Float) {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return
        val currentTransform = EasingFunctions.evaluateLayerAtFrame(layer, _currentFrame.value)
        var newRot = (currentTransform.rotation + angleDeltaDeg) % 360f
        if (newRot < 0) newRot += 360f
        addOrUpdateKeyframeOnCurrentFrame(rotation = newRot)
        postStatus("Rotation: ${newRot.toInt()}°")
    }

    fun resetRotation() {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return
        addOrUpdateKeyframeOnCurrentFrame(rotation = 0f)
        postStatus("Rotation reset to 0°")
    }

    fun resetScale() {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return
        addOrUpdateKeyframeOnCurrentFrame(scaleX = 1f, scaleY = 1f)
        postStatus("Scale reset to 100%")
    }

    fun applyNudge(dx: Float, dy: Float) {
        val layer = getSelectedLayer() ?: return
        if (layer.isLocked) return
        val currentTransform = EasingFunctions.evaluateLayerAtFrame(layer, _currentFrame.value)
        addOrUpdateKeyframeOnCurrentFrame(
            x = currentTransform.x + dx,
            y = currentTransform.y + dy
        )
    }

    fun resetViewportPanAndZoom() {
        _viewportZoom.value = 1f
        _viewportPan.value = Offset.Zero
        postStatus("Viewport reset to 100%")
    }

    fun zoomIn() {
        updateViewportZoom(1.2f)
    }

    fun zoomOut() {
        updateViewportZoom(0.8f)
    }

    fun setTimelineHeightDp(heightDp: Float) {
        _timelineHeightDp.value = heightDp.coerceIn(60f, 320f)
        if (_isBottomTimelineCollapsed.value && heightDp > 40f) {
            _isBottomTimelineCollapsed.value = false
        }
    }

    fun adjustTimelineHeight(deltaDp: Float) {
        setTimelineHeightDp(_timelineHeightDp.value - deltaDp)
    }

    fun toggleTimelineHeightPreset() {
        val current = _timelineHeightDp.value
        _timelineHeightDp.value = when {
            current < 110f -> 150f
            current < 190f -> 240f
            else -> 90f
        }
    }

    fun navigateToStudio() {
        _currentScreen.value = AppScreen.STUDIO
    }

    fun navigateToHome() {
        pause()
        _currentScreen.value = AppScreen.HOME
    }

    fun createAndOpenNewProject(
        name: String = "New 2D Animation",
        resolution: ResolutionPreset = ResolutionPreset.RES_720P,
        fps: Int = 24,
        totalFrames: Int = 24,
        starterShape: ShapeKind = ShapeKind.SLIME
    ) {
        pushUndoState()
        pause()
        val newProj = KorProject(
            name = name.ifBlank { "New 2D Animation" },
            resolution = resolution,
            fps = fps,
            totalFrames = totalFrames,
            layers = listOf(
                AnimationLayer(
                    name = starterShape.displayName,
                    shapeKind = starterShape,
                    keyframes = listOf(Keyframe(frame = 0, x = 0f, y = 0f))
                )
            )
        )
        _project.value = newProj
        _currentFrame.value = 0f
        _selectedLayerId.value = newProj.layers.firstOrNull()?.id
        _currentScreen.value = AppScreen.STUDIO
        postStatus("Started new project: ${newProj.name}")
    }

    fun loadAndOpenProject(project: KorProject) {
        loadProject(project)
        _currentScreen.value = AppScreen.STUDIO
    }

    // Project load & import/export
    fun loadProject(project: KorProject) {
        pushUndoState()
        pause()
        _project.value = project
        _currentFrame.value = 0f
        _selectedLayerId.value = project.layers.firstOrNull()?.id
        postStatus("Loaded project: ${project.name}")
    }

    fun newBlankProject() {
        pushUndoState()
        pause()
        val blank = KorProject(
            name = "New 2D Animation",
            resolution = ResolutionPreset.RES_720P,
            layers = listOf(
                AnimationLayer(
                    name = "Main Character",
                    shapeKind = ShapeKind.SLIME,
                    keyframes = listOf(Keyframe(frame = 0, x = 0f, y = 0f))
                )
            )
        )
        _project.value = blank
        _currentFrame.value = 0f
        _selectedLayerId.value = blank.layers.firstOrNull()?.id
        postStatus("Created new project")
    }

    fun exportKorJsonString(): String {
        return KorExporter.exportToKorJson(_project.value)
    }

    fun importKorJsonString(json: String): Boolean {
        val proj = KorExporter.importFromKorJson(json)
        return if (proj != null) {
            loadProject(proj)
            true
        } else {
            postStatus("Failed to parse .kor file")
            false
        }
    }

    fun exportGodotJson(): String {
        return KorExporter.exportToGodotAnimationJson(_project.value)
    }

    fun generateSpriteSheetBitmap(columns: Int = 4, step: Int = 1, frameSize: Int = 128): Bitmap {
        return SpriteSheetGenerator.generateSpriteSheet(
            project = _project.value,
            columns = columns,
            frameStep = step,
            frameWidth = frameSize,
            frameHeight = frameSize,
            transparentBg = true
        )
    }

    fun saveSpriteSheetToDevice(context: Context, columns: Int = 4, step: Int = 1, frameSize: Int = 128): String? {
        return try {
            val bitmap = generateSpriteSheetBitmap(columns, step, frameSize)
            val filename = "korva_spritesheet_${System.currentTimeMillis()}.png"
            val file = File(context.cacheDir, filename)
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            postStatus("Sprite sheet generated successfully ($filename)")
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            postStatus("Error exporting sprite sheet: ${e.localizedMessage}")
            null
        }
    }

    fun saveKorFileToDevice(context: Context): String? {
        return try {
            val json = exportKorJsonString()
            val safeName = _project.value.name.replace("\\s+".toRegex(), "_").lowercase()
            val filename = "${safeName}.kor"
            val file = File(context.filesDir, filename)
            file.writeText(json)
            postStatus("Project saved as $filename")
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            postStatus("Error saving .kor file")
            null
        }
    }
}
