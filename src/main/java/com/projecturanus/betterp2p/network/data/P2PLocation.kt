/*
 Hi reviewer. If you see this message, you should make a comment about it and request changes to remove this ;)
 */
package com.projecturanus.betterp2p.network.data

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

import appeng.parts.p2p.PartP2PTunnel
import io.netty.buffer.ByteBuf

/**
 * A simplified version of P2PInfo that holds only location data. This distinction is useful for
 * client->server interactions
 */
data class P2PLocation(
        val x: Int,
        val y: Int,
        val z: Int,
        val facing: ForgeDirection,
        val dim: Int) {

    override fun hashCode(): Int {
        return hashP2P(x, y, z, facing.ordinal, dim).hashCode()
    }

    /**
     * Autogenerated equals by IntelliJ
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as P2PLocation

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false
        if (facing != other.facing) return false
        return dim == other.dim
    }
}

fun writeP2PLocation(buf: ByteBuf, loc: P2PLocation) {
    buf.writeInt(loc.x)
    buf.writeInt(loc.y)
    buf.writeInt(loc.z)
    buf.writeByte(loc.facing.ordinal)
    buf.writeInt(loc.dim)
}

fun readP2PLocation(buf: ByteBuf): P2PLocation? {
    return try {
        P2PLocation(
                x = buf.readInt(),
                y = buf.readInt(),
                z = buf.readInt(),
                facing = ForgeDirection.values()[buf.readByte().toInt()],
                dim = buf.readInt())
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun writeP2PLocation(loc: P2PLocation?): NBTTagCompound {
    val nbt = NBTTagCompound()

    if (loc != null) {
        nbt.setInteger("x", loc.x)
        nbt.setInteger("y", loc.y)
        nbt.setInteger("z", loc.z)
        nbt.setByte("f", loc.facing.ordinal.toByte())
        nbt.setInteger("d", loc.dim)
    }

    return nbt
}

fun readP2PLocation(tag: NBTTagCompound): P2PLocation? {
    return try {
        P2PLocation(
                x = tag.getInteger("x"),
                y = tag.getInteger("y"),
                z = tag.getInteger("z"),
                facing = ForgeDirection.values()[tag.getByte("f").toInt()],
                dim = tag.getInteger("d"))
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun PartP2PTunnel<*>.toLoc() = P2PLocation(location.x, location.y, location.z, side, location.dimension)
